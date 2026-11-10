-- =====================================================================
-- 채팅 목록(S-08)용: 내 방 목록 + 마지막 메시지 + 안 읽은 수 + 진행 중인 투표 + 멤버
-- 방마다 여러 번 조회하지 않도록 함수 하나로 내려준다. 내가 속한 방만 나온다.
-- =====================================================================
create function my_rooms()
returns table (
    id uuid,
    name text,
    is_group boolean,
    members jsonb,            -- [{id, name, major}] (나 포함)
    last_sender_name text,    -- 눈송이 메시지는 '눈송이'
    last_content text,
    last_at timestamptz,
    unread_count int,
    has_active_vote boolean
)
language sql stable security definer set search_path = public as $$
    select r.id,
           r.name,
           r.is_group,
           (select coalesce(jsonb_agg(jsonb_build_object('id', p.id, 'name', p.name, 'major', p.major) order by p.name), '[]'::jsonb)
              from room_members rm join profiles p on p.id = rm.user_id
             where rm.room_id = r.id),
           case when lm.id is null then null when lm.sender_id is null then '눈송이' else sp.name end,
           coalesce(lm.content, ''),
           coalesce(lm.created_at, r.created_at),
           (select count(*)::int from messages x
             where x.room_id = r.id and x.created_at > me.last_read_at
               and (x.sender_id is null or x.sender_id <> auth.uid())),
           exists (select 1 from meetups mt where mt.room_id = r.id and mt.status = 'voting')
    from chat_rooms r
    join room_members me on me.room_id = r.id and me.user_id = auth.uid()
    left join lateral (
        select * from messages m where m.room_id = r.id order by m.created_at desc limit 1
    ) lm on true
    left join profiles sp on sp.id = lm.sender_id
    order by coalesce(lm.created_at, r.created_at) desc
$$;
revoke execute on function my_rooms() from public, anon;
grant execute on function my_rooms() to authenticated;
