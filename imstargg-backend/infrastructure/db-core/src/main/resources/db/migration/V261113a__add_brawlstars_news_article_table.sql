-- 공식 블로그 글 테이블
create table if not exists brawlstars_news_article
(
    brawlstars_news_article_id bigint generated always as identity primary key,
    article_key                varchar(255) not null,
    link_url                   varchar(500) not null,
    title                      varchar(255) not null,
    published_at               timestamptz  not null,
    type                       varchar(25)  not null,
    raw_body                   text         not null,
    content                    text         not null,
    created_at                 timestamptz  not null default now(),
    updated_at                 timestamptz  not null default now()
);
