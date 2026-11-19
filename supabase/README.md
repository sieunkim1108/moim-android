
## Edge Functions (학교 메일 인증)
- `functions/send-email-code` : 숙명 웹메일로 6자리 인증번호 발송 (이미 가입된 메일이면 409, 30초 안 재요청은 429)
- `functions/verify-email-code` : 번호 확인 → `{ "verified": true|false }`. 5번 틀리거나 3분 지나면 다시 요청
- 메일 발송은 **Gmail SMTP**(보내는 주소 moimnunsong@gmail.com, 465 포트)를 씁니다. Resend 키가 있으면 대신 Resend로 보냅니다.
- 비밀값은 대시보드 > Edge Functions > Secrets 에만 넣습니다 (앱이나 GitHub에는 넣지 않기)
  - `GMAIL_USER` = 보내는 Gmail 주소
  - `GMAIL_APP_PASSWORD` = Google 앱 비밀번호 16자리 (이름 오타 주의: APP)
  - `EMAIL_CODE_PEPPER` = 아무 긴 랜덤 문자열
- 가입 시 DB 트리거가 30분 안에 인증된 학교 메일인지 다시 확인합니다 (20261119000001 마이그레이션)

## Auth 설정 (대시보드 > Authentication)
- 앱은 `학번@moim.app`이라는 가상 메일로 가입하므로 **Confirm email을 꺼 주세요** (실제 메일 확인은 학교 메일 인증이 대신함)
- 비밀번호 최소 길이는 8 (앱 검사와 같게)

## 앱 연결
`moim-android/local.properties`에 `supabase.url`, `supabase.anonKey` 두 줄을 넣으면 앱이 Supabase로 동작합니다.
