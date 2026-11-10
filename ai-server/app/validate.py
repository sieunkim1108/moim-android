"""
Claude가 뽑은 일정 후보를 코드로 검증한다 (보고서 4.3-4).
- 날짜: 원래 표현(date_expression)을 코드가 다시 계산할 수 있으면 코드 결과를 쓴다.
- 시간: 시험·약속인데 시간이 비어 있으면 되묻는다 (과제 마감·종일 일정은 시간 없어도 됨).
- 알림: 사용자 성향의 알림 시점(notification_prefs)을 붙인다.
"""
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta

from . import dates
from .schemas import ExtractedSchedule, NotificationPrefs, ScheduleCandidate, ScheduleType

MAX_DAYS_AHEAD = 400  # 1년 넘게 뒤 날짜는 잘못 계산한 것으로 본다


@dataclass
class ValidationResult:
    candidates: list[ScheduleCandidate]
    clarify_question: str | None  # 되물을 말 (있으면 카드 대신 질문)


def _parse_hhmm(value: str | None) -> time | None:
    if not value:
        return None
    try:
        return datetime.strptime(value, "%H:%M").time()
    except ValueError:
        return None


def _reminders(kind: ScheduleType, prefs: NotificationPrefs) -> list[int]:
    if kind == ScheduleType.EXAM:
        return [prefs.exam_days_before]
    if kind == ScheduleType.ASSIGNMENT:
        return [prefs.assignment_days_before]
    if kind == ScheduleType.APPOINTMENT:
        return [prefs.appointment_days_before]
    return []


def _needs_time(s: ExtractedSchedule) -> bool:
    return not s.all_day and not s.is_deadline and s.type in (ScheduleType.EXAM, ScheduleType.APPOINTMENT)


def validate(extracted: list[ExtractedSchedule], today: date, prefs: NotificationPrefs) -> ValidationResult:
    candidates: list[ScheduleCandidate] = []
    for s in extracted:
        llm_date: date | None
        try:
            llm_date = date.fromisoformat(s.date)
        except ValueError:
            llm_date = None

        code_date = dates.find_date(s.date_expression, today)
        final_date = code_date or llm_date
        if final_date is None or not (today - timedelta(days=1) <= final_date <= today + timedelta(days=MAX_DAYS_AHEAD)):
            return ValidationResult([], f"'{s.title}'은(는) 며칠이야? 날짜를 한 번만 더 알려줘!")

        start = _parse_hhmm(s.start_time)
        if s.time_expression and (code_time := dates.find_time(s.time_expression)):
            start = code_time
        if start is None and _needs_time(s):
            return ValidationResult([], f"'{s.title}'은(는) 몇 시야? 시간을 알려주면 바로 넣어 둘게!")

        end = _parse_hhmm(s.end_time)
        if start and end and end <= start:
            end = None  # 끝 시간이 시작보다 빠르면 버리고 앱 기본 길이를 쓴다

        candidates.append(
            ScheduleCandidate(
                title=s.title.strip(),
                type=s.type,
                date=final_date,
                start_time=start,
                end_time=end,
                all_day=s.all_day,
                is_deadline=s.is_deadline,
                location=s.location,
                reminder_days_before=_reminders(s.type, prefs),
                date_corrected=llm_date is not None and code_date is not None and code_date != llm_date,
            )
        )
    return ValidationResult(candidates, None)
