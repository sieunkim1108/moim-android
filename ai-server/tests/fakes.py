"""테스트용 가짜 LLM: Claude를 부르지 않고 정해 둔 답을 돌려준다."""
from datetime import date

from app.schemas import (
    CandidateReason,
    Extraction,
    Intent,
    MeetupExplanation,
    MeetupIntent,
    TimetableClass,
    TimetableResult,
)


class FakeLLM:
    def __init__(self, intent: Intent = Intent.CHAT, extraction: Extraction | None = None):
        self.intent = intent
        self.extraction = extraction or Extraction(empathy="", schedules=[])

    async def classify_intent(self, message, history):
        return self.intent

    async def extract(self, message, user, history):
        return self.extraction

    async def stream_chat(self, message, user, history):
        for piece in ["안녕", "! 오늘도 ", "화이팅 💙"]:
            yield piece

    async def read_timetable(self, image_base64, media_type):
        return TimetableResult(
            classes=[TimetableClass(title="자료구조", day_of_week=1, start_time="10:30", end_time="11:45", location="명신관 302")],
            unreadable=False,
        )

    async def meetup_intent(self, message, today: date):
        return MeetupIntent(is_meetup="만나" in message, title=None, period_start=None, period_end=None)

    async def explain_meetup(self, req):
        # 없는 id 하나를 섞어서 서버가 걸러내는지 확인
        return MeetupExplanation(reasons=[CandidateReason(id="ghost", reason="?")] + [
            CandidateReason(id=c.id, reason=f"{c.available_count}명 가능") for c in req.candidates
        ])
