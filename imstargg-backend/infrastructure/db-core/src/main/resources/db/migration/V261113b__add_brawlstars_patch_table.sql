-- 패치일 테이블과 초기 패치일 (2026-11-03, 2026-11-11)
-- 멱등: 같은 날짜가 있으면 넘어간다.
create table if not exists brawlstars_patch
(
    brawlstars_patch_id bigint generated always as identity primary key,
    patch_date          date         not null,
    memo                varchar(255) null,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now()
);
create unique index if not exists uk_brawlstars_patch__patch_date on brawlstars_patch (patch_date);

insert into brawlstars_patch (patch_date)
values ('2026-11-03'), ('2026-11-11')
on conflict (patch_date) do nothing;
