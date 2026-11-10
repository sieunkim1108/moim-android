-- 내부용 함수는 API로 직접 못 부르게 한다 (Supabase 보안 점검 경고 정리)
revoke execute on function handle_new_user() from public, anon, authenticated;
revoke execute on function can_see_schedule_detail(schedules, uuid) from public, anon, authenticated;
-- RLS 정책 안에서 쓰는 함수는 로그인한 사용자만
revoke execute on function is_friend(uuid, uuid) from public, anon;
revoke execute on function is_room_member(uuid, uuid) from public, anon;
grant execute on function is_friend(uuid, uuid) to authenticated;
grant execute on function is_room_member(uuid, uuid) to authenticated;
