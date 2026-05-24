-- V260517__add_feedback_table.sql
create table if not exists feedback (
    id         bigserial   primary key,
    category   varchar(32) not null,
    content    text        not null,
    email      varchar(254),
    checked    boolean     not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_feedback_created_at on feedback (created_at desc);
create index if not exists idx_feedback_checked    on feedback (checked);
