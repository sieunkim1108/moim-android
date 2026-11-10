"""
앱이 보낸 Supabase 로그인 토큰을 확인한다. 로그인한 사용자만 AI 서버를 쓸 수 있다.
Supabase Auth의 /auth/v1/user에 토큰을 보내서 진짜 사용자인지 묻는다 (anon key만 사용).
"""
import httpx
from fastapi import Depends, Header, HTTPException, status

from .config import Settings, load_settings


async def current_user_id(
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(load_settings),
) -> str:
    if settings.dev_skip_auth:
        return "dev-user"
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "로그인이 필요해요")
    async with httpx.AsyncClient(timeout=5) as client:
        r = await client.get(
            f"{settings.supabase_url}/auth/v1/user",
            headers={"Authorization": authorization, "apikey": settings.supabase_anon_key},
        )
    if r.status_code != 200:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "로그인이 만료됐어요")
    return r.json()["id"]
