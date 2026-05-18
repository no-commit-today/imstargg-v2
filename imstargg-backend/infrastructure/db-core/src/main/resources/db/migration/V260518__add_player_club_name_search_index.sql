-- player.name / club.name 대소문자 무관 prefix 검색 인덱스
-- LIKE 'xxx%' 매칭에 최적화된 varchar_pattern_ops B-tree.
-- 멱등 작성: create index if not exists.

create index if not exists ix_player__lower_name
    on player (lower(name) varchar_pattern_ops);

create index if not exists ix_club__lower_name
    on club (lower(name) varchar_pattern_ops);
