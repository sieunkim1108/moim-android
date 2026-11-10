"""ClaudeLLM이 Claude API에 보내는 요청 모양 확인 (네트워크 없이 가짜 HTTP로)."""
import json
from datetime import date

import anthropic
import httpx2 as httpx
import pytest

from app.llm import ClaudeLLM
from app.schemas import Intent, UserContext

sent: list[dict] = []


def _handler(answer: dict):
    def handle(request: httpx.Request) -> httpx.Response:
        sent.append({"headers": dict(request.headers), "body": json.loads(request.content)})
        body = json.loads(request.content)
        return httpx.Response(200, json={
            "id": "msg_1", "type": "message", "role": "assistant", "model": body["model"],
            "content": [{"type": "text", "text": json.dumps(answer, ensure_ascii=False)}],
            "stop_reason": "end_turn", "stop_sequence": None,
            "usage": {"input_tokens": 1, "output_tokens": 1},
        })
    return handle


def _llm(answer: dict) -> ClaudeLLM:
    http = httpx.AsyncClient(transport=httpx.MockTransport(_handler(answer)))
    return ClaudeLLM(anthropic.AsyncAnthropic(api_key="test", http_client=http, max_retries=0))


@pytest.mark.asyncio
async def test_intent_uses_haiku_with_schema():
    sent.clear()
    intent = await _llm({"intent": "register"}).classify_intent("내일 시험이야", [])
    assert intent == Intent.REGISTER
    body = sent[0]["body"]
    assert body["model"] == "claude-haiku-5-5"
    assert body["output_config"]["format"]["type"] == "json_schema"
    assert body["system"][0]["cache_control"] == {"type": "ephemeral"}


@pytest.mark.asyncio
async def test_extract_uses_sonnet_with_fallback_and_context_in_user_turn():
    sent.clear()
    user = UserContext(name="김눈송", today=date(2026, 11, 5))
    result = await _llm({"empathy": "화이팅", "schedules": []}).extract("다음 주 월요일 시험", user, [])
    assert result.empathy == "화이팅"
    req = sent[0]
    assert req["body"]["model"] == "claude-sonnet-5-5"
    assert req["body"]["fallbacks"] == "default"
    assert "server-side-fallback-2026-07-01" in req["headers"]["anthropic-beta"]
    assert req["body"]["output_config"]["effort"] == "low"
    # 날짜는 시스템 프롬프트가 아니라 user 메시지에 (캐시 유지)
    assert "2026-11-05" in req["body"]["messages"][-1]["content"]
    assert "2026-11-05" not in json.dumps(req["body"]["system"], ensure_ascii=False)
