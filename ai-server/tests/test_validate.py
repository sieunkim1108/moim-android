from datetime import date, time

from app.schemas import ExtractedSchedule, NotificationPrefs, ScheduleType
from app.validate import validate

TODAY = date(2026, 11, 5)
PREFS = NotificationPrefs(exam_days_before=3, assignment_days_before=1, appointment_days_before=0)


def sched(**kw) -> ExtractedSchedule:
    base = dict(title="데이터 분석 시험", type=ScheduleType.EXAM, date_expression="다음 주 월요일", date="2026-11-09",
                start_time="10:00", end_time=None, time_expression="10시", all_day=False, is_deadline=False, location=None)
    base.update(kw)
    return ExtractedSchedule(**base)


def test_code_corrects_wrong_llm_date():
    r = validate([sched(date="2026-11-16")], TODAY, PREFS)
    assert r.clarify_question is None
    assert r.candidates[0].date == date(2026, 11, 9)
    assert r.candidates[0].date_corrected
    assert r.candidates[0].reminder_days_before == [3]


def test_llm_date_kept_when_expression_not_parseable():
    r = validate([sched(date_expression="중간고사 첫날", date="2026-11-20")], TODAY, PREFS)
    assert r.candidates[0].date == date(2026, 11, 20)
    assert not r.candidates[0].date_corrected


def test_missing_time_for_exam_asks_back():
    r = validate([sched(start_time=None, time_expression=None)], TODAY, PREFS)
    assert r.candidates == []
    assert "몇 시" in r.clarify_question


def test_deadline_without_time_is_ok():
    r = validate([sched(title="과제 제출", type=ScheduleType.ASSIGNMENT, date_expression="금요일까지", date="2026-11-06",
                        start_time=None, time_expression=None, is_deadline=True)], TODAY, PREFS)
    assert r.clarify_question is None
    assert r.candidates[0].date == date(2026, 11, 6)
    assert r.candidates[0].reminder_days_before == [1]


def test_code_time_overrides_llm_time():
    # Claude가 오후 3시를 03:00으로 잘못 줘도 원문 "3시"로 다시 계산 → 15:00
    r = validate([sched(start_time="03:00", time_expression="3시")], TODAY, PREFS)
    assert r.candidates[0].start_time == time(15, 0)


def test_far_future_date_asks_back():
    r = validate([sched(date_expression="언젠가", date="2030-01-01")], TODAY, PREFS)
    assert "며칠" in r.clarify_question


def test_end_before_start_dropped():
    r = validate([sched(end_time="09:00")], TODAY, PREFS)
    assert r.candidates[0].end_time is None
