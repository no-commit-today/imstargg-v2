-- V260522__redesign_feedback_to_rating.sql
-- 피드백 도메인 재설계: 카테고리/이메일 제거, 5단계 rating + submittedAt 도입
drop table if exists feedback;

create table feedback (
    id           bigserial   primary key,
    rating       varchar(32) not null,
    content      text        not null,
    checked      boolean     not null default false,
    submitted_at timestamptz not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index ix_feedback__checked__submittedat
    on feedback (checked, submitted_at desc);
