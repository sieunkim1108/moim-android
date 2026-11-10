"""
moim AI 서버 (FastAPI).

POST /chat           눈송이 대화. SSE로 intent → (delta… | reply + card | clarify) → done
POST /timetable      에브리타임 시간표 캡처 → 매주 반복 수업 후보 (사용자가 확인해야 저장)
POST /meetup/intent  단톡방 메시지가 모임 잡기인지 (Haiku)
POST /meetup/explain 알고리즘이 고른 후보 시간에 이유 문장 붙이기 (점수는 바꾸지 않음)

일정은 여기서 저장하지 않는다. 앱이 확인 카드를 보여 주고, 사용자가 [등록하기]를 눌러야 저장한다.
"""
import json
import logging
from collections.abc import AsyncIterator
from functools import lru_cache

import anthropic
from fastapi import Depends, FastAPI, HTTPException
from sse_starlette.sse import EventSourceResponse

from .auth import current_user_id
from .graph import build_graph
from .llm import LLM, ClaudeLLM, LLMRefused
from .schemas import (
    ChatRequest,
    Intent,
    MeetupExplainRequest,
    MeetupExplanation,
    MeetupIntent,
    MeetupIntentRequest,
    TimetableRequest,
    TimetableResult,
)

log = logging.getLogger("moim.ai")
app = FastAPI(title="moim AI server")

SORRY = "앗, 눈송이가 잠깐 헷갈렸어 😢 조금 뒤에 다시 말해 줄래?"


@lru_cache
def get_llm() -> LLM:
    return ClaudeLLM()


def _event(name: str, data) -> dict:
    return {"event": name, "data": json.dumps(data, ensure_ascii=False, default=str)}


@app.get("/health")
async def health():
    return {"ok": True}


@app.post("/chat")
async def chat(req: ChatRequest, user_id: str = Depends(current_user_id), llm: LLM = Depends(get_llm)):
    graph = build_graph(llm)

    async def events() -> AsyncIterator[dict]:
        try:
            state = await graph.ainvoke({"message": req.message, "user": req.user, "history": req.history})
            intent = state["intent"]
            yield _event("intent", {"intent": intent.value})

            if state.get("clarify"):
                yield _event("clarify", {"question": state["clarify"]})
            elif intent == Intent.REGISTER and state.get("candidates"):
                yield _event("reply", {"text": state.get("empathy", "")})
                yield _event("card", {
                    "card_type": "schedule_confirm",
                    "schedules": [c.model_dump(mode="json") for c in state["candidates"]],
                })
            else:
                # 일반 대화·조회 등: 공감 문장을 글자 단위로 흘려보낸다
                async for text in llm.stream_chat(req.message, req.user, req.history):
                    yield _event("delta", {"text": text})
        except LLMRefused:
            yield _event("reply", {"text": "그 얘기는 눈송이가 도와주기 어려워. 다른 일정 얘기는 언제든 해 줘!"})
        except anthropic.APIError as e:
            log.warning("claude error: %s", e)
            yield _event("error", {"text": SORRY})
        yield _event("done", {})

    return EventSourceResponse(events())


async def _call(coro):
    try:
        return await coro
    except LLMRefused:
        raise HTTPException(422, "이 요청은 처리할 수 없어요")
    except anthropic.APIError as e:
        log.warning("claude error: %s", e)
        raise HTTPException(503, SORRY)


@app.post("/timetable", response_model=TimetableResult)
async def timetable(req: TimetableRequest, user_id: str = Depends(current_user_id), llm: LLM = Depends(get_llm)):
    # 캡처 이미지만 받는다. 에브리타임 계정이나 크롤링은 쓰지 않는다.
    return await _call(llm.read_timetable(req.image_base64, req.media_type))


@app.post("/meetup/intent", response_model=MeetupIntent)
async def meetup_intent(req: MeetupIntentRequest, user_id: str = Depends(current_user_id), llm: LLM = Depends(get_llm)):
    return await _call(llm.meetup_intent(req.message, req.today))


@app.post("/meetup/explain", response_model=MeetupExplanation)
async def meetup_explain(req: MeetupExplainRequest, user_id: str = Depends(current_user_id), llm: LLM = Depends(get_llm)):
    result: MeetupExplanation = await _call(llm.explain_meetup(req))
    # 모델이 없는 후보 id를 만들거나 빠뜨려도 앱이 깨지지 않게 정리
    known = {c.id for c in req.candidates}
    reasons = {r.id: r for r in result.reasons if r.id in known}
    result.reasons = [reasons[c.id] for c in req.candidates if c.id in reasons]
    return result
