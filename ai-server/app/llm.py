"""
Claude 호출을 한곳에 모은다.
- Sonnet 5.5: 일정 추출, 대화, 시간표 인식, 모임 추천 이유
- Haiku 5.5: 의도 분류, 단톡방 모임 의도 감지 (가볍고 빠른 일)
- Structured Outputs(Pydantic)로 JSON 모양을 보장하고, 시스템 프롬프트는 캐시한다.
- Sonnet 요청에는 서버 측 fallback을 켠다: 안전 분류기가 거절하면 같은 요청을 다른 모델로 다시 시도.
테스트에서는 이 클래스 대신 FakeLLM을 넣는다 (tests/fakes.py).
"""
import json
from collections.abc import AsyncIterator
from datetime import date
from typing import Protocol, TypeVar

import anthropic
from pydantic import BaseModel

from . import prompts
from .config import MODEL_LIGHT, MODEL_MAIN
from .schemas import (
    ChatTurn,
    Extraction,
    Intent,
    IntentResult,
    MeetupExplainRequest,
    MeetupExplanation,
    MeetupIntent,
    TimetableResult,
    UserContext,
)

T = TypeVar("T", bound=BaseModel)

FALLBACK_BETA = "server-side-fallback-2026-07-01"
WEEKDAY_KO = "월화수목금토일"


class LLMRefused(Exception):
    """모델이 안전 문제로 답하지 않았을 때"""


class LLM(Protocol):
    async def classify_intent(self, message: str, history: list[ChatTurn]) -> Intent: ...
    async def extract(self, message: str, user: UserContext, history: list[ChatTurn]) -> Extraction: ...
    def stream_chat(self, message: str, user: UserContext, history: list[ChatTurn]) -> AsyncIterator[str]: ...
    async def read_timetable(self, image_base64: str, media_type: str) -> TimetableResult: ...
    async def meetup_intent(self, message: str, today: date) -> MeetupIntent: ...
    async def explain_meetup(self, req: MeetupExplainRequest) -> MeetupExplanation: ...


def _system(text: str) -> list[dict]:
    # 고정된 시스템 프롬프트에 캐시 지점을 둔다
    return [{"type": "text", "text": text, "cache_control": {"type": "ephemeral"}}]


def _context_line(user: UserContext) -> str:
    """매번 바뀌는 정보는 user 메시지 앞에 붙인다 (캐시를 깨지 않게)"""
    p = user.notification_prefs
    return (
        f"[오늘] {user.today.isoformat()} ({WEEKDAY_KO[user.today.weekday()]}요일), 시간대 Asia/Seoul\n"
        f"[사용자] 이름 {user.name}, 일정 성향 {user.planning_type}, "
        f"알림: 시험 {p.exam_days_before}일 전 / 과제 {p.assignment_days_before}일 전 / 약속 {p.appointment_days_before}일 전"
    )


def _messages(history: list[ChatTurn], last_user: str) -> list[dict]:
    msgs = [{"role": "assistant" if t.role == "bot" else "user", "content": t.content} for t in history[-10:]]
    # 대화는 user로 시작하고 user/assistant가 번갈아야 한다
    while msgs and msgs[0]["role"] != "user":
        msgs.pop(0)
    merged: list[dict] = []
    for m in msgs:
        if merged and merged[-1]["role"] == m["role"]:
            merged[-1]["content"] += "\n" + m["content"]
        else:
            merged.append(dict(m))
    if merged and merged[-1]["role"] == "user":
        merged[-1]["content"] += "\n" + last_user
    else:
        merged.append({"role": "user", "content": last_user})
    return merged


class ClaudeLLM:
    def __init__(self, client: anthropic.AsyncAnthropic | None = None):
        self.client = client or anthropic.AsyncAnthropic()

    async def _parse_main(self, system: str, messages: list[dict], schema: type[T], max_tokens: int = 4000) -> T:
        response = await self.client.beta.messages.parse(
            model=MODEL_MAIN,
            max_tokens=max_tokens,
            system=_system(system),
            messages=messages,
            output_format=schema,
            output_config={"effort": "low"},  # 추출은 빨라야 해서 낮은 effort
            betas=[FALLBACK_BETA],
            fallbacks="default",
        )
        return self._parsed(response, schema)

    async def _parse_light(self, system: str, messages: list[dict], schema: type[T]) -> T:
        response = await self.client.messages.parse(
            model=MODEL_LIGHT,
            max_tokens=1000,
            system=_system(system),
            messages=messages,
            output_format=schema,
            output_config={"effort": "low"},
        )
        return self._parsed(response, schema)

    @staticmethod
    def _parsed(response, schema: type[T]) -> T:
        if response.stop_reason == "refusal":
            raise LLMRefused()
        if response.parsed_output is not None:
            return response.parsed_output
        text = next(b.text for b in response.content if b.type == "text")
        return schema.model_validate(json.loads(text))

    async def classify_intent(self, message: str, history: list[ChatTurn]) -> Intent:
        recent = "\n".join(f"{t.role}: {t.content}" for t in history[-4:])
        content = (f"[최근 대화]\n{recent}\n\n" if recent else "") + f"[분류할 말]\n{message}"
        result = await self._parse_light(prompts.INTENT_SYSTEM, [{"role": "user", "content": content}], IntentResult)
        return result.intent

    async def extract(self, message: str, user: UserContext, history: list[ChatTurn]) -> Extraction:
        last = f"{_context_line(user)}\n\n[사용자 말]\n{message}"
        return await self._parse_main(prompts.EXTRACT_SYSTEM, _messages(history, last), Extraction)

    async def stream_chat(self, message: str, user: UserContext, history: list[ChatTurn]) -> AsyncIterator[str]:
        last = f"{_context_line(user)}\n\n{message}"
        async with self.client.beta.messages.stream(
            model=MODEL_MAIN,
            max_tokens=2000,
            system=_system(prompts.CHAT_SYSTEM),
            messages=_messages(history, last),
            output_config={"effort": "low"},
            betas=[FALLBACK_BETA],
            fallbacks="default",
        ) as stream:
            async for text in stream.text_stream:
                yield text

    async def read_timetable(self, image_base64: str, media_type: str) -> TimetableResult:
        messages = [{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": media_type, "data": image_base64}},
                {"type": "text", "text": "이 시간표의 수업을 모두 읽어 줘."},
            ],
        }]
        return await self._parse_main(prompts.TIMETABLE_SYSTEM, messages, TimetableResult, max_tokens=8000)

    async def meetup_intent(self, message: str, today: date) -> MeetupIntent:
        content = f"[오늘] {today.isoformat()} ({WEEKDAY_KO[today.weekday()]}요일)\n[메시지]\n{message}"
        return await self._parse_light(prompts.MEETUP_INTENT_SYSTEM, [{"role": "user", "content": content}], MeetupIntent)

    async def explain_meetup(self, req: MeetupExplainRequest) -> MeetupExplanation:
        content = f"[모임] {req.title}\n[후보]\n" + req.model_dump_json(include={"candidates"})
        return await self._parse_main(prompts.MEETUP_EXPLAIN_SYSTEM, [{"role": "user", "content": content}], MeetupExplanation)
