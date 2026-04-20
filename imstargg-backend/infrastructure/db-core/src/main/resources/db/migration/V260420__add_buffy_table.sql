-- buffy 테이블 신설: 브롤러별 버피 등록 여부를 어드민에서 관리
create table if not exists buffy
(
    buffy_id              bigint generated always as identity primary key,
    brawler_brawlstars_id bigint      not null,
    type                  varchar(25) not null,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);
create unique index if not exists uk_buffy__brawlerbrawlstarsid_type on buffy (brawler_brawlstars_id, type);
