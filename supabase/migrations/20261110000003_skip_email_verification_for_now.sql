-- 학교 메일 인증은 나중에 다시 켠다 (사용자 요청: 우선 학번·비밀번호로 쉽게 가입)
-- 다시 켤 때는 20261105000001_init_schema.sql의 handle_new_user 검사 부분을 되살리면 된다.
create or replace function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    meta   jsonb := new.raw_user_meta_data;
    ptype  planning_type := coalesce((meta ->> 'planning_type')::planning_type, 'early');
    v_email text := lower(meta ->> 'school_email');
begin
    insert into profiles (id, student_id, school_email, name, birth_date, college, major, planning_type, default_visibility)
    values (
        new.id,
        meta ->> 'student_id',
        v_email,
        meta ->> 'name',
        (meta ->> 'birth_date')::date,
        meta ->> 'college',
        meta ->> 'major',
        ptype,
        coalesce((meta ->> 'default_visibility')::visibility, 'friends')
    );

    insert into notification_prefs (user_id, exam_days_before, assignment_days_before, appointment_days_before)
    values (
        new.id,
        coalesce((meta ->> 'exam_days_before')::int, 7),
        coalesce((meta ->> 'assignment_days_before')::int, 7),
        coalesce((meta ->> 'appointment_days_before')::int, 1)
    );
    return new;
end $$;
revoke execute on function handle_new_user() from public, anon, authenticated;
