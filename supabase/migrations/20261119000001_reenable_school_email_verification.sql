-- 학교 메일 인증 다시 켜기 (Gmail 발송 준비 완료, 2026-11-19)
-- 가입할 때 30분 안에 인증을 마친 학교 메일인지 DB가 다시 확인한다 (앱만 믿지 않기).
-- 되돌릴 때: 20261110000003_skip_email_verification_for_now.sql 내용을 다시 실행
create or replace function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    meta   jsonb := new.raw_user_meta_data;
    ptype  planning_type := coalesce((meta ->> 'planning_type')::planning_type, 'early');
    v_email text := lower(meta ->> 'school_email');
begin
    if not exists (
        select 1 from email_verifications v
        where v.email = v_email and v.verified_at is not null and v.verified_at > now() - interval '30 minutes'
    ) then
        raise exception '학교 이메일 인증이 필요해요';
    end if;

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
