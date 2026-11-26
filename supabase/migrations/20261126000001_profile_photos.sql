-- =====================================================================
-- 프로필 사진
-- 1) profiles.avatar_url: 사진 주소 (없으면 이름 첫 글자 동그라미)
-- 2) Storage 'avatars' 버킷: 사진 파일 저장소. 경로는 avatars/<내 uid>/<시각>.jpg
-- 3) 친구 목록·채팅방 멤버 함수가 avatar_url도 돌려주게 다시 만든다
-- Supabase 대시보드 → SQL Editor에서 이 파일 전체를 한 번 실행한다.
-- =====================================================================

alter table profiles add column if not exists avatar_url text;

-- 공개 버킷: 주소만 알면 누구나 사진을 볼 수 있다 (카톡 프로필처럼). 2MB, 이미지 파일만.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 2097152, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;

-- 올리기·바꾸기·지우기는 "내 uid 폴더" 안에서만
create policy "avatars: 본인 폴더 읽기" on storage.objects for select to authenticated
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars: 본인 폴더 올리기" on storage.objects for insert to authenticated
    with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars: 본인 폴더 바꾸기" on storage.objects for update to authenticated
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars: 본인 폴더 지우기" on storage.objects for delete to authenticated
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

-- 돌려주는 칸이 바뀌면 create or replace가 안 되므로 지웠다가 다시 만든다
drop function if exists my_friends();
create function my_friends()
returns table (id uuid, name text, major text, avatar_url text)
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.major, p.avatar_url
    from friendships f
    join profiles p on p.id = case when f.user_id = auth.uid() then f.friend_id else f.user_id end
    where f.status = 'accepted' and auth.uid() in (f.user_id, f.friend_id)
    order by p.name
$$;

drop function if exists room_member_profiles(uuid);
create function room_member_profiles(p_room uuid)
returns table (id uuid, name text, major text, student_id text, avatar_url text)
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.major, p.student_id, p.avatar_url
    from room_members m join profiles p on p.id = m.user_id
    where m.room_id = p_room and is_room_member(p_room, auth.uid())
$$;

revoke execute on function my_friends() from public, anon;
revoke execute on function room_member_profiles(uuid) from public, anon;
grant execute on function my_friends() to authenticated;
grant execute on function room_member_profiles(uuid) to authenticated;
