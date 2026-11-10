-- =====================================================================
-- moim 초기 스키마 (최종 보고서 4.4 데이터 저장 방식 기준)
-- 모든 테이블에 RLS(Row Level Security)를 켠다.
-- RLS: "로그인한 사용자가 어떤 행을 읽고 쓸 수 있는지"를 DB가 직접 검사하는 기능.
--      앱에는 anon key만 넣어도, 정책에 맞지 않는 행은 DB가 아예 돌려주지 않는다.
-- =====================================================================

create extension if not exists vector;      -- pgvector (개인화 메모리)
create extension if not exists pgcrypto;    -- 인증번호 해시

-- ---------------------------------------------------------------------
-- 0. 공통 타입
-- ---------------------------------------------------------------------
create type planning_type    as enum ('early', 'three_day', 'cramming', 'planner');
create type visibility       as enum ('public', 'friends', 'group', 'private');
create type schedule_type    as enum ('exam', 'assignment', 'appointment', 'meetup', 'class', 'etc');
create type schedule_source  as enum ('chat', 'manual', 'timetable', 'meetup');
create type friend_status    as enum ('pending', 'accepted');
create type meetup_status    as enum ('voting', 'confirmed');

-- ---------------------------------------------------------------------
-- 1. 학교 이메일 인증 (Edge Function만 service_role로 읽고 쓴다)
-- ---------------------------------------------------------------------
create table email_verifications (
    email        text primary key check (email ~* '^[A-Za-z0-9._%+-]+@sookmyung\.ac\.kr$'),
    code_hash    text        not null,           -- 인증번호는 해시로만 저장
    expires_at   timestamptz not null,           -- 발송 + 3분
    attempts     int         not null default 0, -- 5번 틀리면 재요청 필요
    verified_at  timestamptz,
    created_at   timestamptz not null default now()
);
alter table email_verifications enable row level security;
-- 정책 없음 = 앱(anon/authenticated)은 접근 불가. Edge Function(service_role)만 사용.

-- ---------------------------------------------------------------------
-- 2. 프로필
-- ---------------------------------------------------------------------
create table profiles (
    id                 uuid primary key references auth.users (id) on delete cascade,
    student_id         text not null unique check (student_id ~ '^\d{7}$'),
    school_email       text not null unique,
    name               text not null check (length(trim(name)) > 0),
    birth_date         date not null,
    college            text not null,
    major              text not null,
    planning_type      planning_type not null default 'early',
    default_visibility visibility    not null default 'friends',
    created_at         timestamptz   not null default now()
);
alter table profiles enable row level security;

-- 본인만 읽고 고친다. 친구/검색용 최소 정보는 아래 security definer 함수로만 내보낸다.
create policy "profiles: 본인 조회" on profiles for select using (id = auth.uid());
create policy "profiles: 본인 수정" on profiles for update using (id = auth.uid()) with check (id = auth.uid());

-- 학번·학교 이메일은 바꿀 수 없다 (S-11)
create function profiles_lock_identity() returns trigger language plpgsql as $$
begin
    if new.student_id <> old.student_id or new.school_email <> old.school_email then
        raise exception '학번과 학교 이메일은 수정할 수 없어요';
    end if;
    return new;
end $$;
create trigger profiles_lock_identity before update on profiles
    for each row execute function profiles_lock_identity();

-- ---------------------------------------------------------------------
-- 3. 유형별 알림 시점
-- ---------------------------------------------------------------------
create table notification_prefs (
    user_id                 uuid primary key references profiles (id) on delete cascade,
    exam_days_before        int not null default 7 check (exam_days_before in (1, 3, 7)),
    assignment_days_before  int not null default 7 check (assignment_days_before in (1, 3, 7)),
    appointment_days_before int not null default 1 check (appointment_days_before in (0, 1, 3))
);
alter table notification_prefs enable row level security;
create policy "prefs: 본인" on notification_prefs for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- 4. 회원가입 → 프로필 자동 생성
--    앱은 supabase.auth.signUp(email = "학번@moim.app", data = {...})만 부른다.
--    인증을 마친 학교 이메일인지 DB가 다시 확인한다 (앱만 믿지 않기).
-- ---------------------------------------------------------------------
create function handle_new_user() returns trigger
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
create trigger on_auth_user_created after insert on auth.users
    for each row execute function handle_new_user();

-- 학번 중복 확인 (로그인 전에도 부를 수 있게 anon 허용)
create function is_student_id_available(p_student_id text) returns boolean
language sql stable security definer set search_path = public as $$
    select not exists (select 1 from profiles where student_id = p_student_id)
$$;
grant execute on function is_student_id_available(text) to anon, authenticated;

-- ---------------------------------------------------------------------
-- 5. 친구
-- ---------------------------------------------------------------------
create table friendships (
    user_id     uuid not null references profiles (id) on delete cascade, -- 요청 보낸 사람
    friend_id   uuid not null references profiles (id) on delete cascade, -- 요청 받은 사람
    status      friend_status not null default 'pending',
    message     text check (length(message) <= 100),                      -- 한 줄 메시지 (S-12)
    created_at  timestamptz not null default now(),
    primary key (user_id, friend_id),
    check (user_id <> friend_id)
);
alter table friendships enable row level security;

create policy "friendships: 당사자 조회" on friendships for select
    using (auth.uid() in (user_id, friend_id));
create policy "friendships: 내가 요청" on friendships for insert
    with check (user_id = auth.uid() and status = 'pending');
create policy "friendships: 받은 사람만 수락" on friendships for update
    using (friend_id = auth.uid()) with check (friend_id = auth.uid() and status = 'accepted');
create policy "friendships: 당사자 삭제(거절·취소·끊기)" on friendships for delete
    using (auth.uid() in (user_id, friend_id));

-- 두 사람이 친구인지 (양방향)
create function is_friend(a uuid, b uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from friendships
        where status = 'accepted'
          and ((user_id = a and friend_id = b) or (user_id = b and friend_id = a))
    )
$$;

-- ---------------------------------------------------------------------
-- 6. 채팅방 / 멤버 / 메시지
-- ---------------------------------------------------------------------
create table chat_rooms (
    id          uuid primary key default gen_random_uuid(),
    name        text,
    is_group    boolean not null default true,
    created_by  uuid not null references profiles (id),
    created_at  timestamptz not null default now()
);
create table room_members (
    room_id      uuid not null references chat_rooms (id) on delete cascade,
    user_id      uuid not null references profiles (id) on delete cascade,
    last_read_at timestamptz not null default now(),
    primary key (room_id, user_id)
);
create table messages (
    id            uuid primary key default gen_random_uuid(),
    room_id       uuid not null references chat_rooms (id) on delete cascade,
    sender_id     uuid references profiles (id),   -- null = 눈송이(bot)
    content       text not null default '',
    card_type     text check (card_type in ('meetup_suggestion', 'meetup_vote', 'meetup_confirmed')),
    card_payload  jsonb,
    created_at    timestamptz not null default now()
);
create index messages_room_time on messages (room_id, created_at);

alter table chat_rooms   enable row level security;
alter table room_members enable row level security;
alter table messages     enable row level security;

create function is_room_member(p_room uuid, p_user uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (select 1 from room_members where room_id = p_room and user_id = p_user)
$$;

-- 만든 사람은 멤버로 들어가기 전에도 방을 볼 수 있어야 한다 (생성 직후 insert ... returning, 본인·친구 초대)
create policy "rooms: 멤버나 만든 사람만 조회" on chat_rooms for select using (
    created_by = auth.uid() or is_room_member(id, auth.uid())
);
create policy "rooms: 누구나 생성(본인 명의)" on chat_rooms for insert with check (created_by = auth.uid());
create policy "rooms: 만든 사람 이름 변경" on chat_rooms for update using (created_by = auth.uid());

create policy "members: 같은 방 멤버만 조회" on room_members for select using (is_room_member(room_id, auth.uid()));
-- 방 만든 사람은 친구를 초대할 수 있고, 본인은 자기 읽음 시각만 바꾼다
create policy "members: 방장이 친구 초대" on room_members for insert with check (
    exists (select 1 from chat_rooms r where r.id = room_id and r.created_by = auth.uid())
    and (user_id = auth.uid() or is_friend(auth.uid(), user_id))
);
create policy "members: 본인 읽음 갱신" on room_members for update using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "members: 본인 나가기" on room_members for delete using (user_id = auth.uid());

create policy "messages: 멤버만 조회" on messages for select using (is_room_member(room_id, auth.uid()));
-- 사람은 자기 이름으로만 쓴다. 눈송이 메시지(sender_id null)는 서버(service_role)만 쓴다.
create policy "messages: 멤버만 작성" on messages for insert
    with check (sender_id = auth.uid() and is_room_member(room_id, auth.uid()) and card_type is null);

-- 같은 방 멤버의 이름·전공 (말풍선, 멤버 보기)
create function room_member_profiles(p_room uuid)
returns table (id uuid, name text, major text, student_id text)
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.major, p.student_id
    from room_members m join profiles p on p.id = m.user_id
    where m.room_id = p_room and is_room_member(p_room, auth.uid())
$$;

-- ---------------------------------------------------------------------
-- 7. 일정 / 그룹공개 대상 / 알림
-- ---------------------------------------------------------------------
create table schedules (
    id          uuid primary key default gen_random_uuid(),
    owner_id    uuid not null references profiles (id) on delete cascade,
    title       text not null check (length(trim(title)) > 0),
    type        schedule_type   not null,
    start_at    timestamptz     not null,
    end_at      timestamptz     not null,
    all_day     boolean         not null default false,
    is_deadline boolean         not null default false,   -- 과제 마감처럼 "이때까지"
    repeat_rule text,                                     -- 예: RRULE:FREQ=WEEKLY;BYDAY=MO,WE (시간표)
    visibility  visibility      not null default 'friends',
    source      schedule_source not null default 'manual',
    location    text,
    memo        text,
    created_at  timestamptz     not null default now(),
    check (end_at >= start_at)
);
create index schedules_owner_time on schedules (owner_id, start_at);

-- 그룹공개 일정을 볼 수 있는 단체방들 (S-07-1 그룹 선택)
create table schedule_shared_rooms (
    schedule_id uuid not null references schedules (id) on delete cascade,
    room_id     uuid not null references chat_rooms (id) on delete cascade,
    primary key (schedule_id, room_id)
);

create table reminders (
    id          uuid primary key default gen_random_uuid(),
    schedule_id uuid not null references schedules (id) on delete cascade,
    remind_at   timestamptz not null,
    sent        boolean not null default false
);
create index reminders_due on reminders (remind_at) where not sent;

alter table schedules             enable row level security;
alter table schedule_shared_rooms enable row level security;
alter table reminders             enable row level security;

-- 원본 일정 테이블은 "본인만". 친구에게는 아래 함수가 가려서 내보낸다.
-- (RLS는 행 단위라 "제목만 숨기기"가 안 돼서, 친구 조회는 전용 함수로 분리)
create policy "schedules: 본인" on schedules for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

create policy "shared_rooms: 일정 주인" on schedule_shared_rooms for all
    using (exists (select 1 from schedules s where s.id = schedule_id and s.owner_id = auth.uid()))
    with check (
        exists (select 1 from schedules s where s.id = schedule_id and s.owner_id = auth.uid())
        and is_room_member(room_id, auth.uid())   -- 내가 속한 방에만 공개 가능
    );

create policy "reminders: 일정 주인" on reminders for all
    using (exists (select 1 from schedules s where s.id = schedule_id and s.owner_id = auth.uid()))
    with check (exists (select 1 from schedules s where s.id = schedule_id and s.owner_id = auth.uid()));

-- viewer가 owner의 일정 제목을 볼 수 있는지 (공개 범위 규칙)
create function can_see_schedule_detail(p_schedule schedules, p_viewer uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select case p_schedule.visibility
        when 'public'  then true
        when 'friends' then is_friend(p_schedule.owner_id, p_viewer)
        when 'group'   then exists (
            select 1 from schedule_shared_rooms sr
            where sr.schedule_id = p_schedule.id and is_room_member(sr.room_id, p_viewer)
        )
        else false
    end
$$;

-- 친구 캘린더 보기 (S-07): 친구만 호출 가능.
-- 볼 수 없는 일정은 제목 = "OOO님의 일정입니다", 유형·장소·메모 없이 시간만.
create function friend_schedules(p_friend uuid, p_from timestamptz, p_to timestamptz)
returns table (
    id uuid, title text, type schedule_type, start_at timestamptz, end_at timestamptz,
    all_day boolean, is_deadline boolean, visibility visibility, location text, is_hidden boolean
)
language plpgsql stable security definer set search_path = public as $$
#variable_conflict use_column
declare
    owner_name text;
begin
    if not is_friend(p_friend, auth.uid()) then
        raise exception '친구의 캘린더만 볼 수 있어요';
    end if;
    select name into owner_name from profiles where profiles.id = p_friend;
    return query
        select s.id,
               case when v.ok then s.title else owner_name || '님의 일정입니다' end,
               case when v.ok then s.type else 'etc'::schedule_type end,
               s.start_at, s.end_at, s.all_day, s.is_deadline,
               case when v.ok then s.visibility else 'private'::visibility end,
               case when v.ok then s.location end,
               not v.ok
        from schedules s
        cross join lateral (select can_see_schedule_detail(s, auth.uid()) as ok) v
        where s.owner_id = p_friend and s.start_at < p_to and s.end_at > p_from
        order by s.start_at;
end $$;

-- 모임 조율 (S-10): 같은 방 멤버들의 "바쁜 시간"만. 제목은 공개 범위에 맞을 때만, 아니면 null(🔒 비공개).
create function room_busy_blocks(p_room uuid, p_from timestamptz, p_to timestamptz)
returns table (user_id uuid, start_at timestamptz, end_at timestamptz, label text)
language sql stable security definer set search_path = public as $$
    select s.owner_id, s.start_at, s.end_at,
           case when s.owner_id = auth.uid() or can_see_schedule_detail(s, auth.uid()) then s.title end
    from schedules s
    join room_members m on m.user_id = s.owner_id and m.room_id = p_room
    where is_room_member(p_room, auth.uid())
      and not s.is_deadline and not s.all_day
      and s.start_at < p_to and s.end_at > p_from
$$;

-- ---------------------------------------------------------------------
-- 8. 친구 검색 / 친구 목록 (S-12) - 최소 정보만
-- ---------------------------------------------------------------------
create function search_student(p_student_id text)
returns table (id uuid, name text, student_id text, major text, relation text, common_rooms text[])
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.student_id, p.major,
           case
               when p.id = auth.uid() then 'me'
               when is_friend(p.id, auth.uid()) then 'friend'
               when exists (select 1 from friendships f where f.user_id = auth.uid() and f.friend_id = p.id) then 'requested'
               when exists (select 1 from friendships f where f.user_id = p.id and f.friend_id = auth.uid()) then 'incoming'
               else 'none'
           end,
           array(
               select r.name from chat_rooms r
               where r.is_group and is_room_member(r.id, p.id) and is_room_member(r.id, auth.uid())
           )
    from profiles p
    where p.student_id = p_student_id and auth.uid() is not null
$$;

create function my_friends()
returns table (id uuid, name text, major text)
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.major
    from friendships f
    join profiles p on p.id = case when f.user_id = auth.uid() then f.friend_id else f.user_id end
    where f.status = 'accepted' and auth.uid() in (f.user_id, f.friend_id)
    order by p.name
$$;

create function incoming_friend_requests()
returns table (requester_id uuid, name text, student_id text, major text, message text, created_at timestamptz)
language sql stable security definer set search_path = public as $$
    select p.id, p.name, p.student_id, p.major, f.message, f.created_at
    from friendships f join profiles p on p.id = f.user_id
    where f.friend_id = auth.uid() and f.status = 'pending'
    order by f.created_at desc
$$;

-- ---------------------------------------------------------------------
-- 9. 눈송이 1:1 대화 (메인 화면)
-- ---------------------------------------------------------------------
create table bot_messages (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references profiles (id) on delete cascade,
    role          text not null check (role in ('user', 'bot')),
    content       text not null default '',
    card_type     text check (card_type in ('schedule_confirm', 'reminder_suggestion')),
    card_payload  jsonb,
    created_at    timestamptz not null default now()
);
create index bot_messages_user_time on bot_messages (user_id, created_at);
alter table bot_messages enable row level security;
create policy "bot_messages: 본인 조회" on bot_messages for select using (user_id = auth.uid());
-- 사용자는 자기 말만 저장. 눈송이 답장은 AI 서버(service_role)가 쓴다.
create policy "bot_messages: 본인 발화 저장" on bot_messages for insert
    with check (user_id = auth.uid() and role = 'user');
-- 확인 카드 상태(등록 완료 등)는 본인이 갱신
create policy "bot_messages: 본인 카드 갱신" on bot_messages for update
    using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- 10. 모임 / 후보 / 투표
-- ---------------------------------------------------------------------
create table meetups (
    id            uuid primary key default gen_random_uuid(),
    room_id       uuid not null references chat_rooms (id) on delete cascade,
    title         text not null,
    period_start  date not null,
    period_end    date not null,
    duration_min  int  not null default 120 check (duration_min between 30 and 240),
    status        meetup_status not null default 'voting',
    confirmed_candidate_id uuid,
    confirmed_at  timestamptz,
    created_by    uuid not null references profiles (id),
    created_at    timestamptz not null default now()
);
create table meetup_candidates (
    id              uuid primary key default gen_random_uuid(),
    meetup_id       uuid not null references meetups (id) on delete cascade,
    start_at        timestamptz not null,
    end_at          timestamptz not null,
    score           numeric not null,          -- 알고리즘이 계산 (LLM 아님)
    available_count int not null,
    reason          text                        -- LLM이 쓴 추천 이유 문장
);
create table meetup_votes (
    candidate_id uuid not null references meetup_candidates (id) on delete cascade,
    user_id      uuid not null references profiles (id) on delete cascade,
    created_at   timestamptz not null default now(),
    primary key (candidate_id, user_id)
);
alter table meetups enable row level security;
alter table meetup_candidates enable row level security;
alter table meetup_votes enable row level security;

create policy "meetups: 방 멤버 조회" on meetups for select using (is_room_member(room_id, auth.uid()));
create policy "meetups: 방 멤버 생성" on meetups for insert
    with check (created_by = auth.uid() and is_room_member(room_id, auth.uid()) and status = 'voting');
-- 확정(status 변경)은 confirm_meetup 함수로만 → update 정책 없음

create policy "candidates: 방 멤버 조회" on meetup_candidates for select
    using (exists (select 1 from meetups m where m.id = meetup_id and is_room_member(m.room_id, auth.uid())));
create policy "candidates: 모임 만든 사람 등록" on meetup_candidates for insert
    with check (exists (select 1 from meetups m where m.id = meetup_id and m.created_by = auth.uid() and m.status = 'voting'));

create policy "votes: 방 멤버 조회" on meetup_votes for select using (exists (
    select 1 from meetup_candidates c join meetups m on m.id = c.meetup_id
    where c.id = candidate_id and is_room_member(m.room_id, auth.uid())
));
create policy "votes: 방 멤버가 본인 표만" on meetup_votes for insert with check (
    user_id = auth.uid() and exists (
        select 1 from meetup_candidates c join meetups m on m.id = c.meetup_id
        where c.id = candidate_id and m.status = 'voting' and is_room_member(m.room_id, auth.uid())
    )
);
create policy "votes: 본인 표 취소" on meetup_votes for delete using (user_id = auth.uid());

-- 모임 확정: 참여자 전원 캘린더에 모임 일정 + 전날 알림을 한 번에 넣는다 (트랜잭션)
create function confirm_meetup(p_meetup uuid, p_candidate uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
    m meetups;
    c meetup_candidates;
    member record;
    new_id uuid;
begin
    select * into m from meetups where id = p_meetup for update;
    if not found or not is_room_member(m.room_id, auth.uid()) then
        raise exception '이 모임을 확정할 수 없어요';
    end if;
    if m.status = 'confirmed' then
        raise exception '이미 확정된 모임이에요';
    end if;
    select * into c from meetup_candidates where id = p_candidate and meetup_id = p_meetup;
    if not found then
        raise exception '후보 시간을 찾을 수 없어요';
    end if;

    for member in select user_id from room_members where room_id = m.room_id loop
        insert into schedules (owner_id, title, type, start_at, end_at, visibility, source)
        values (member.user_id, m.title, 'meetup', c.start_at, c.end_at, 'friends', 'meetup')
        returning id into new_id;
        insert into reminders (schedule_id, remind_at) values (new_id, c.start_at - interval '1 day');
    end loop;

    update meetups set status = 'confirmed', confirmed_candidate_id = c.id, confirmed_at = now() where id = p_meetup;

    insert into messages (room_id, sender_id, content, card_type, card_payload)
    values (m.room_id, null, '모임 시간이 정해졌어! 다들 캘린더에 넣어 뒀어 💙', 'meetup_confirmed',
            jsonb_build_object('meetup_id', m.id, 'start_at', c.start_at, 'end_at', c.end_at));
end $$;

-- ---------------------------------------------------------------------
-- 11. 개인화 메모리 (pgvector) - AI 서버(service_role)만 쓴다
-- ---------------------------------------------------------------------
create table user_memory (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references profiles (id) on delete cascade,
    content    text not null,
    embedding  vector(1024),
    created_at timestamptz not null default now()
);
alter table user_memory enable row level security;
create policy "memory: 본인 조회" on user_memory for select using (user_id = auth.uid());
create policy "memory: 본인 삭제" on user_memory for delete using (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- 12. Realtime: 채팅·투표 변경을 앱에 실시간으로 보낸다 (RLS가 그대로 적용됨)
-- ---------------------------------------------------------------------
alter publication supabase_realtime add table messages, meetup_votes, meetups, room_members, friendships;

-- ---------------------------------------------------------------------
-- 13. 함수 실행 권한: 로그인한 사용자만
-- ---------------------------------------------------------------------
revoke execute on function friend_schedules(uuid, timestamptz, timestamptz) from public, anon;
revoke execute on function room_busy_blocks(uuid, timestamptz, timestamptz) from public, anon;
revoke execute on function search_student(text) from public, anon;
revoke execute on function my_friends() from public, anon;
revoke execute on function incoming_friend_requests() from public, anon;
revoke execute on function room_member_profiles(uuid) from public, anon;
revoke execute on function confirm_meetup(uuid, uuid) from public, anon;
grant execute on function friend_schedules(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function room_busy_blocks(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function search_student(text) to authenticated;
grant execute on function my_friends() to authenticated;
grant execute on function incoming_friend_requests() to authenticated;
grant execute on function room_member_profiles(uuid) to authenticated;
grant execute on function confirm_meetup(uuid, uuid) to authenticated;
