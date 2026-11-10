"""서버 설정. 비밀값은 전부 환경변수(.env)로만 받는다. 코드·GitHub에 키를 넣지 않는다."""
import os
from dataclasses import dataclass
from zoneinfo import ZoneInfo

SEOUL = ZoneInfo("Asia/Seoul")

# 보고서 4.6: Sonnet = 대화·추출·추천 이유, Haiku = 가벼운 분류(의도 감지)
MODEL_MAIN = "claude-sonnet-5-5"
MODEL_LIGHT = "claude-haiku-5-5"


@dataclass(frozen=True)
class Settings:
    anthropic_api_key: str | None  # 없으면 SDK가 다른 인증 수단을 찾는다
    supabase_url: str
    supabase_anon_key: str
    dev_skip_auth: bool  # 로컬 개발용: 로그인 토큰 검사를 건너뜀 (배포에서는 절대 켜지 말 것)


def load_settings() -> Settings:
    return Settings(
        anthropic_api_key=os.getenv("ANTHROPIC_API_KEY"),
        supabase_url=os.getenv("SUPABASE_URL", "").rstrip("/"),
        supabase_anon_key=os.getenv("SUPABASE_ANON_KEY", ""),
        dev_skip_auth=os.getenv("DEV_SKIP_AUTH") == "1",
    )
