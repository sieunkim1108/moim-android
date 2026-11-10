from datetime import date, time

from app import dates

# 2026-11-05는 목요일
TODAY = date(2026, 11, 5)


def test_relative_days():
    assert dates.find_date("오늘", TODAY) == TODAY
    assert dates.find_date("내일 시험", TODAY) == date(2026, 11, 6)
    assert dates.find_date("모레", TODAY) == date(2026, 11, 7)
    assert dates.find_date("3일 뒤", TODAY) == date(2026, 11, 8)


def test_weekdays():
    assert dates.find_date("다음 주 월요일", TODAY) == date(2026, 11, 9)
    assert dates.find_date("담주 월욜", TODAY) == date(2026, 11, 9)
    assert dates.find_date("이번 주 화요일", TODAY) == date(2026, 11, 3)
    assert dates.find_date("금요일까지", TODAY) == date(2026, 11, 6)
    assert dates.find_date("목요일", TODAY) == TODAY  # 오늘 포함 다가오는 목요일
    assert dates.find_date("다다음 주 수요일", TODAY) == date(2026, 11, 18)


def test_month_day_rolls_to_next_year():
    assert dates.find_date("11월 9일", TODAY) == date(2026, 11, 9)
    assert dates.find_date("3월 2일", TODAY) == date(2027, 3, 2)
    assert dates.find_date("2월 30일", TODAY) is None
    assert dates.find_date("12/24", TODAY) == date(2026, 12, 24)


def test_times():
    assert dates.find_time("오후 3시 반") == time(15, 30)
    assert dates.find_time("3시") == time(15, 0)  # 오전/오후 없이 1~7시는 오후
    assert dates.find_time("10시") == time(10, 0)
    assert dates.find_time("저녁 7시 20분") == time(19, 20)
    assert dates.find_time("오전 12시") == time(0, 0)
    assert dates.find_time("정오") == time(12, 0)
    assert dates.find_time("14:00") == time(14, 0)
    assert dates.find_time("3시간 공부") is None
