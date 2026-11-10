-- 개발 단계용: 학번 가상 이메일(@moim.app) 계정은 가입 즉시 이메일 확인 처리
-- (Supabase 'Confirm email' 설정은 켜둔 채로, 학교 메일 인증은 나중에 다시 붙인다)
-- 되돌릴 때: drop trigger auto_confirm_virtual_email on auth.users; drop function public.auto_confirm_virtual_email();
create or replace function public.auto_confirm_virtual_email() returns trigger
language plpgsql security definer set search_path = public as $$
begin
    if new.email like '%@moim.app' and new.email_confirmed_at is null then
        new.email_confirmed_at := now();
    end if;
    return new;
end $$;
revoke execute on function public.auto_confirm_virtual_email() from public, anon, authenticated;

drop trigger if exists auto_confirm_virtual_email on auth.users;
create trigger auto_confirm_virtual_email
    before insert on auth.users
    for each row execute function public.auto_confirm_virtual_email();

-- 이미 가입했지만 확인 안 된 @moim.app 계정도 확인 처리
update auth.users set email_confirmed_at = now()
where email like '%@moim.app' and email_confirmed_at is null;
