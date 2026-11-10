"""앱 ↔ AI 서버, AI 서버 ↔ Claude 사이에서 주고받는 데이터 모양 (Pydantic)."""
from datetime import date, time
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class Strict(BaseModel):
    # Structured Output 스키마는 정의하지 않은 키를 허용하지 않아야 한다
    model_config = ConfigDict(extra="forbid")


# ---------------- 앱 → 서버 요청 ----------------

class NotificationPrefs(BaseModel):
    exam_days_before: int = 7
    assignment_days_before: int = 7
    appointment_days_before: int = 1


class UserContext(BaseModel):
    """앱이 매 요청에 같이 보내는 사용자 정보 (보고서 4.3-1)"""
    name: str
    planning_type: str = "early"  # early / three_day / cramming / planner
    notification_prefs: NotificationPrefs = NotificationPrefs()
    today: date  # 앱 기준 오늘 날짜 (한국 시간)


class ChatTurn(BaseModel):
    role: str  # "user" | "bot"
    content: str


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=1000)
    user: UserContext
    history: list[ChatTurn] = Field(default_factory=list, max_length=20)  # 최근 대화 몇 개


# ---------------- 의도 분류 (Haiku) ----------------

class Intent(str, Enum):
    REGISTER = "register"  # 일정 등록
    QUERY = "query"        # 일정 조회
    UPDATE = "update"      # 일정 수정
    DELETE = "delete"      # 일정 삭제
    CHAT = "chat"          # 일반 대화


class IntentResult(Strict):
    intent: Intent


# ---------------- 일정 추출 (Sonnet) ----------------

class ScheduleType(str, Enum):
    EXAM = "exam"
    ASSIGNMENT = "assignment"
    APPOINTMENT = "appointment"
    CLASS = "class"
    ETC = "etc"


class ExtractedSchedule(Strict):
    title: str = Field(description="일정 이름. 예: 데이터 분석 시험")
    type: ScheduleType
    date_expression: str = Field(description="사용자가 날짜를 말한 원래 표현 그대로. 예: 다음 주 월요일")
    date: str = Field(description="계산한 날짜 YYYY-MM-DD")
    start_time: str | None = Field(description="HH:MM 24시간제. 말하지 않았으면 null")
    end_time: str | None = Field(description="HH:MM. 말하지 않았으면 null")
    time_expression: str | None = Field(description="시간을 말한 원래 표현. 예: 오후 3시 반. 없으면 null")
    all_day: bool
    is_deadline: bool = Field(description="과제 제출처럼 '~까지'인 일정이면 true")
    location: str | None


class Extraction(Strict):
    empathy: str = Field(description="눈송이 말투의 짧은 공감 한두 문장")
    schedules: list[ExtractedSchedule]


# ---------------- 서버 → 앱 응답 ----------------

class ScheduleCandidate(BaseModel):
    """확인 카드에 보여 줄 일정. 사용자가 [등록하기]를 눌러야 앱이 저장한다."""
    title: str
    type: ScheduleType
    date: date
    start_time: time | None
    end_time: time | None
    all_day: bool
    is_deadline: bool
    location: str | None
    reminder_days_before: list[int]
    date_corrected: bool = False  # 서버가 날짜를 다시 계산해서 고쳤는지 (디버그용)


# ---------------- 시간표 이미지 (Sonnet 비전) ----------------

class TimetableClass(Strict):
    title: str
    day_of_week: int = Field(description="월=1 ... 일=7")
    start_time: str = Field(description="HH:MM")
    end_time: str = Field(description="HH:MM")
    location: str | None


class TimetableResult(Strict):
    classes: list[TimetableClass]
    unreadable: bool = Field(description="시간표 이미지가 아니거나 읽을 수 없으면 true")


class TimetableRequest(BaseModel):
    image_base64: str = Field(max_length=8_000_000)
    media_type: str = Field(pattern=r"^image/(png|jpeg|webp)$")


# ---------------- 모임 (단톡방) ----------------

class MeetupIntentRequest(BaseModel):
    message: str = Field(min_length=1, max_length=500)
    today: date


class MeetupIntent(Strict):
    is_meetup: bool = Field(description="여러 명이 만날 시간을 정하려는 말이면 true")
    title: str | None = Field(description="모임 이름 추측. 예: 팀플 회의. 없으면 null")
    period_start: str | None = Field(description="원하는 기간 시작 YYYY-MM-DD. 말 안 했으면 null")
    period_end: str | None


class MeetupCandidateIn(BaseModel):
    """점수는 앱/서버의 알고리즘(MeetupScheduler)이 이미 계산해서 보낸다. LLM은 점수를 바꾸지 않는다."""
    id: str
    start_at: str
    end_at: str
    score: float
    available_count: int
    member_count: int
    notes: list[str] = Field(default_factory=list, max_length=10)  # 예: "모두 수업 끝난 직후"


class MeetupExplainRequest(BaseModel):
    title: str
    candidates: list[MeetupCandidateIn] = Field(min_length=1, max_length=5)


class CandidateReason(Strict):
    id: str
    reason: str = Field(description="한 문장, 40자 안팎")


class MeetupExplanation(Strict):
    reasons: list[CandidateReason]
