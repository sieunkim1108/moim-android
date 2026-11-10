# moim

숙명여대 학생을 위한 AI 일정 관리 앱 (눈송이 챗봇 + 친구 캘린더 + 단톡방 모임 조율).

| 폴더 | 내용 |
|---|---|
| `android/` | Android 앱 (Kotlin, Jetpack Compose, MVVM + Clean Architecture, Hilt) |
| `supabase/` | DB 마이그레이션(RLS), 학교 메일 인증 Edge Functions |
| `ai-server/` | AI 서버 (FastAPI + LangGraph, Claude Sonnet 5.5 / Haiku 5.5) |

각 폴더의 README에 실행 방법이 있어요.

## 비밀값
API 키(Claude, Supabase service_role)는 코드나 GitHub에 넣지 않고 환경변수로만 씁니다.
앱에는 Supabase anon key만 `android/local.properties`에 넣어요 (깃에 안 올라감).
