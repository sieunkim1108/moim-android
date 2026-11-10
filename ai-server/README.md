# moim AI 서버 (FastAPI + LangGraph)

보고서 4.1·4.3 기준. 눈송이 대화, 일정 추출, 시간표 이미지 인식, 모임 추천 이유를 담당합니다.

| 역할 | 모델 |
|---|---|
| 대화, 일정 추출, 시간표 인식, 모임 추천 이유 | Claude Sonnet 5.5 (`claude-sonnet-5-5`) |
| 의도 분류, 단톡방 모임 의도 감지 | Claude Haiku 5.5 (`claude-haiku-5-5`) |

## 흐름 (POST /chat)
1. 앱이 메시지 + 사용자 정보(이름, 성향, 알림 설정, 오늘 날짜)를 보냄
2. Haiku가 의도 분류 (등록 / 조회 / 수정 / 삭제 / 대화)
3. 등록이면 Sonnet이 JSON 스키마(Structured Output)로 일정 후보 + 공감 문장 생성
4. **서버 코드(`app/dates.py`, `app/validate.py`)가 날짜·시간을 다시 계산**하고, 시간이 비면 되묻기
5. SSE 이벤트로 응답: `intent` → `reply` + `card`(확인 카드) 또는 `clarify` 또는 `delta`(대화 스트리밍) → `done`
6. **저장은 앱이 사용자의 [등록하기] 후에만** 합니다. 서버는 일정을 저장하지 않아요

그 밖의 API
- `POST /timetable` 에브리타임 **캡처 이미지**만 받아 수업 후보를 돌려줌 (계정·크롤링 없음)
- `POST /meetup/intent` 단톡방 메시지가 모임 잡기인지 (Haiku)
- `POST /meetup/explain` 알고리즘이 고른 후보에 이유 한 문장씩 (점수·순서는 안 바꿈)

모든 API는 앱의 Supabase 로그인 토큰(`Authorization: Bearer ...`)이 있어야 합니다.

## 실행
```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env   # 값 채우기 (ANTHROPIC_API_KEY는 여기에만!)
set -a; source .env; set +a
uvicorn app.main:app --reload --port 8000
```

## 테스트
```bash
pytest -q
```
Claude를 부르지 않는 가짜 LLM(`tests/fakes.py`)으로 날짜 계산·검증·API 흐름을 확인합니다.
`tests/test_llm_request.py`는 실제로 보내는 요청 모양(모델, 스키마, 캐시, fallback)을 가짜 HTTP로 확인합니다.

## 참고
- Sonnet 요청에는 서버 측 fallback(`fallbacks: "default"`)을 켜 두었습니다. 안전 분류기가 거절하면 같은 요청을 다른 모델로 한 번 더 시도해요. 필요 없으면 `app/llm.py`에서 빼면 됩니다.
- 시스템 프롬프트는 고정하고 `cache_control`을 붙였습니다. 오늘 날짜처럼 바뀌는 값은 user 메시지에 넣어 캐시가 깨지지 않게 했어요.
