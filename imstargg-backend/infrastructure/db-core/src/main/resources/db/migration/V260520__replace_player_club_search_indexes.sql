-- player / club 검색을 case-sensitive startsWith 으로 단순화.
-- V260518 의 lower(name) 인덱스를 단순 name varchar_pattern_ops 로 교체하고,
-- brawlstars_tag prefix 검색용 varchar_pattern_ops 인덱스를 추가.
-- 멱등 작성: drop index / create index if (not) exists.

drop index if exists ix_player__lower_name;
drop index if exists ix_club__lower_name;

create index if not exists ix_player__name
    on player (name varchar_pattern_ops);

create index if not exists ix_club__name
    on club (name varchar_pattern_ops);

create index if not exists ix_player__brawlstars_tag
    on player (brawlstars_tag varchar_pattern_ops);

create index if not exists ix_club__brawlstars_tag
    on club (brawlstars_tag varchar_pattern_ops);
