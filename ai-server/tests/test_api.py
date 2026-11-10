import json
import os

os.environ["DEV_SKIP_AUTH"] = "1"

from fastapi.testclient import TestClient  # noqa: E402

from app.main import app, get_llm  # noqa: E402
from app.schemas import Extraction, ExtractedSchedule, Intent, ScheduleType  # noqa: E402
from tests.fakes import FakeLLM  # noqa: E402

USER = {"name": "김눈송", "planning_type": "early", "today": "2026-11-05",
        "notification_prefs": {"exam_days_before": 7, "assignment_days_before": 7, "appointment_days_before": 1}}


def sse(text: str) -> list[tuple[str, dict]]:
    out, name = [], None
    for line in text.splitlines():
        if line.startswith("event:"):
            name = line.split(":", 1)[1].strip()
        elif line.startswith("data:"):
            out.append((name, json.loads(line.split(":", 1)[1])))
    return out


def client_with(llm) -> TestClient:
    app.dependency_overrides[get_llm] = lambda: llm
    return TestClient(app)


def test_register_returns_card_not_saved():
    extraction = Extraction(empathy="시험 준비 화이팅!", schedules=[ExtractedSchedule(
        title="데이터 분석 시험", type=ScheduleType.EXAM, date_expression="다음 주 월요일", date="2026-11-09",
        start_time="10:00", end_time=None, time_expression="10시", all_day=False, is_deadline=False, location=None)])
    c = client_with(FakeLLM(Intent.REGISTER, extraction))
    events = sse(c.post("/chat", json={"message": "다음 주 월요일 10시 시험", "user": USER}).text)
    names = [n for n, _ in events]
    assert names == ["intent", "reply", "card", "done"]
    card = events[2][1]
    assert card["card_type"] == "schedule_confirm"
    assert card["schedules"][0]["date"] == "2026-11-09"
    assert card["schedules"][0]["reminder_days_before"] == [7]


def test_register_without_time_asks_back():
    extraction = Extraction(empathy="", schedules=[ExtractedSchedule(
        title="친구 약속", type=ScheduleType.APPOINTMENT, date_expression="내일", date="2026-11-06",
        start_time=None, end_time=None, time_expression=None, all_day=False, is_deadline=False, location=None)])
    c = client_with(FakeLLM(Intent.REGISTER, extraction))
    events = sse(c.post("/chat", json={"message": "내일 친구 약속", "user": USER}).text)
    assert [n for n, _ in events] == ["intent", "clarify", "done"]


def test_register_with_no_schedule_falls_back_to_chat():
    c = client_with(FakeLLM(Intent.REGISTER))
    events = sse(c.post("/chat", json={"message": "그냥 피곤해", "user": USER}).text)
    assert events[0] == ("intent", {"intent": "chat"})
    assert "".join(d["text"] for n, d in events if n == "delta") == "안녕! 오늘도 화이팅 💙"


def test_meetup_explain_filters_unknown_ids():
    c = client_with(FakeLLM())
    body = {"title": "팀플 회의", "candidates": [
        {"id": "a", "start_at": "2026-11-09T15:00:00+09:00", "end_at": "2026-11-09T17:00:00+09:00",
         "score": 0.9, "available_count": 4, "member_count": 4}]}
    r = c.post("/meetup/explain", json=body).json()
    assert r == {"reasons": [{"id": "a", "reason": "4명 가능"}]}


def test_timetable_and_meetup_intent():
    c = client_with(FakeLLM())
    r = c.post("/timetable", json={"image_base64": "aGk=", "media_type": "image/png"}).json()
    assert r["classes"][0]["title"] == "자료구조"
    assert c.post("/meetup/intent", json={"message": "언제 만나?", "today": "2026-11-05"}).json()["is_meetup"]


def test_requires_login_when_auth_on(monkeypatch):
    monkeypatch.setenv("DEV_SKIP_AUTH", "0")
    c = client_with(FakeLLM())
    assert c.post("/meetup/intent", json={"message": "언제 만나?", "today": "2026-11-05"}).status_code == 401
