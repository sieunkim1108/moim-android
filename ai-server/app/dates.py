"""
한국어 날짜·시간 표현 → 실제 날짜 (보고서 4.3-4: "서버 코드가 상대 날짜를 다시 계산해 검증").
앱의 KoreanDateParser.kt와 같은 규칙이다. Claude가 준 날짜를 그대로 믿지 않고 여기서 다시 계산한다.
"""
import re
from datetime import date, time, timedelta

WEEKDAYS = {"월": 0, "화": 1, "수": 2, "목": 3, "금": 4, "토": 5, "일": 6}

MONTH_DAY = re.compile(r"(\d{1,2})\s*월\s*(\d{1,2})\s*일")
SLASH_DATE = re.compile(r"(?<![\d/])(\d{1,2})/(\d{1,2})(?![\d/])")
RELATIVE_DAY = re.compile(r"(오늘|내일|낼|모레|글피)")
IN_DAYS = re.compile(r"(\d{1,2})\s*일\s*(?:뒤|후)")
WEEKDAY = re.compile(r"(?:(다다음\s?주|다음\s?주|담주|이번\s?주|저번\s?주)\s*)?([월화수목금토일])(?:요일|욜)")

CLOCK_TIME = re.compile(r"(오전|오후|아침|낮|저녁|밤|새벽)?\s*(\d{1,2}):(\d{2})")
KOREAN_TIME = re.compile(r"(오전|오후|아침|낮|저녁|밤|새벽)?\s*(\d{1,2})\s*시(?![간험])(?:\s*(반)|\s*(\d{1,2})\s*분)?")


def _safe_date(today: date, month: int, day: int) -> date | None:
    """월/일 → 날짜. 한 달 넘게 지난 날짜면 내년으로 본다. 없는 날짜(2/30)는 None."""
    try:
        d = date(today.year, month, day)
    except ValueError:
        return None
    if d < today - timedelta(days=30):
        try:
            d = date(today.year + 1, month, day)
        except ValueError:
            return None
    return d


def resolve_weekday(today: date, weekday: int, qualifier: str | None) -> date:
    """'다음 주 월요일' → 날짜. 주는 월요일부터 시작한다."""
    monday = today - timedelta(days=today.weekday())
    q = (qualifier or "").replace(" ", "")
    if q in ("다음주", "담주"):
        return monday + timedelta(weeks=1, days=weekday)
    if q == "다다음주":
        return monday + timedelta(weeks=2, days=weekday)
    if q == "이번주":
        return monday + timedelta(days=weekday)
    if q == "저번주":
        return monday - timedelta(weeks=1) + timedelta(days=weekday)
    # 그냥 "금요일" = 오늘 포함 다가오는 금요일
    return today + timedelta(days=(weekday - today.weekday()) % 7)


def find_date(text: str, today: date) -> date | None:
    """구체적인 표현이 우선: 11월 9일 > 11/9 > 내일 > N일 뒤 > 금요일"""
    for m in MONTH_DAY.finditer(text):
        if d := _safe_date(today, int(m[1]), int(m[2])):
            return d
    for m in SLASH_DATE.finditer(text):
        if d := _safe_date(today, int(m[1]), int(m[2])):
            return d
    if m := RELATIVE_DAY.search(text):
        offset = {"오늘": 0, "내일": 1, "낼": 1, "모레": 2, "글피": 3}[m[1]]
        return today + timedelta(days=offset)
    if m := IN_DAYS.search(text):
        return today + timedelta(days=int(m[1]))
    if m := WEEKDAY.search(text):
        return resolve_weekday(today, WEEKDAYS[m[2]], m[1])
    return None


def _to_time(period: str | None, hour: int, minute: int) -> time | None:
    if hour > 24 or minute > 59:
        return None
    if period in ("오전", "아침", "새벽"):
        hour = 0 if hour == 12 else hour
    elif period in ("오후", "낮", "저녁", "밤"):
        hour = hour + 12 if hour < 12 else hour
    else:
        hour = hour + 12 if 1 <= hour <= 7 else hour % 24  # 오전/오후 없이 1~7시면 오후
    return time(hour % 24, minute)


def find_time(text: str) -> time | None:
    if "정오" in text:
        return time(12, 0)
    if m := CLOCK_TIME.search(text):
        return _to_time(m[1], int(m[2]), int(m[3]))
    if m := KOREAN_TIME.search(text):
        minute = 30 if m[3] else int(m[4]) if m[4] else 0
        return _to_time(m[1], int(m[2]), minute)
    return None
