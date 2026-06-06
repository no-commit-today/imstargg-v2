-- 브롤스타즈 API 스펙(swagger) 스냅샷 변경 추적 테이블
create table if not exists brawlstars_api_spec
(
    brawlstars_api_spec_id bigint generated always as identity primary key,
    content                text        not null,
    collected_at           timestamptz not null,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
