
## Edge Functions (학교 메일 인증)
- `functions/send-email-code` : 숙명 웹메일로 6자리 인증번호 발송 (이미 가입된 메일이면 409, 30초 안 재요청은 429)
- `functions/verify-email-code` : 번호 확인 → `{ "verified": true|false }`. 5번 틀리거나 3분 지나면 다시 요청
- 메일 발송은 Resend API를 씁니다. 배포 전에 비밀값을 등록하세요 (앱이나 GitHub에는 넣지 않기)
```bash
supabase secrets set RESEND_API_KEY=... EMAIL_FROM="moim <no-reply@보낼도메인>" EMAIL_CODE_PEPPER=<아무 긴 랜덤 문자열>
supabase functions deploy send-email-code
supabase functions deploy verify-email-code
```

## Auth 설정 (대시보드 > Authentication)
- 앱은 `학번@moim.app`이라는 가상 메일로 가입하므로 **Confirm email을 꺼 주세요** (실제 메일 확인은 학교 메일 인증이 대신함)
- 비밀번호 최소 길이는 8 (앱 검사와 같게)

## 앱 연결
`moim-android/local.properties`에 `supabase.url`, `supabase.anonKey` 두 줄을 넣으면 앱이 Supabase로 동작합니다.
