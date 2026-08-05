-- 플레이어 갱신 쓰기 경로 8개 테이블 PK 를 IDENTITY → SEQUENCE(increment 100) 로 전환한다.
-- Hibernate 는 IDENTITY 에서 JDBC insert 배칭을 비활성화한다. 신규 플레이어 갱신 1건이 약 1,100 row
-- (player_brawler 105 + 액세서리 5종 800 + battle 25 + battle_team_player 210) 를 단건 INSERT 로 넣어
-- 6~9.5초가 걸렸다. SEQUENCE(pooled, allocationSize=100) 전환으로 INSERT 배칭을 활성화한다
-- (엔티티 @SequenceGenerator 와 일치 필수). V260707b(통계 테이블 전환)와 같은 패턴.
-- `set default nextval` 은 IDENTITY 를 기대하는 구버전 앱과의 호환 안전망이다 (DDL 선적용 → 앱 배포).
-- setval 은 max(id)+100 으로 초기화해 배포 전환 구간의 잔여 insert 와도 충돌하지 않는다.
-- 멱등: 재실행 시 setval 만 +100 재상승 (무해).
-- 주의: 이 프로젝트는 수동 적용(Flyway 미사용)이다. setval 은 실행 시점 max(id) 기준이라 앞으로만 이동하나,
--       신버전 앱이 이미 캐시한 id 블록을 소진하기 전 재적용하면 극히 드물게 채번 충돌 여지가 있으니
--       재적용은 갱신(renewal) 처리 중이 아닐 때 수행한다.

create sequence if not exists player_brawler_seq increment by 100 owned by player_brawler.player_brawler_id;
alter table player_brawler alter column player_brawler_id drop identity if exists;
alter table player_brawler alter column player_brawler_id set default nextval('player_brawler_seq');
select setval('player_brawler_seq', (select coalesce(max(player_brawler_id), 0) + 100 from player_brawler));

create sequence if not exists player_brawler_gear_seq increment by 100 owned by player_brawler_gear.player_brawler_gear_id;
alter table player_brawler_gear alter column player_brawler_gear_id drop identity if exists;
alter table player_brawler_gear alter column player_brawler_gear_id set default nextval('player_brawler_gear_seq');
select setval('player_brawler_gear_seq', (select coalesce(max(player_brawler_gear_id), 0) + 100 from player_brawler_gear));

create sequence if not exists player_brawler_star_power_seq increment by 100 owned by player_brawler_star_power.player_brawler_star_power_id;
alter table player_brawler_star_power alter column player_brawler_star_power_id drop identity if exists;
alter table player_brawler_star_power alter column player_brawler_star_power_id set default nextval('player_brawler_star_power_seq');
select setval('player_brawler_star_power_seq', (select coalesce(max(player_brawler_star_power_id), 0) + 100 from player_brawler_star_power));

create sequence if not exists player_brawler_gadget_seq increment by 100 owned by player_brawler_gadget.player_brawler_gadget_id;
alter table player_brawler_gadget alter column player_brawler_gadget_id drop identity if exists;
alter table player_brawler_gadget alter column player_brawler_gadget_id set default nextval('player_brawler_gadget_seq');
select setval('player_brawler_gadget_seq', (select coalesce(max(player_brawler_gadget_id), 0) + 100 from player_brawler_gadget));

create sequence if not exists player_brawler_hyper_charge_seq increment by 100 owned by player_brawler_hyper_charge.player_brawler_hyper_charge_id;
alter table player_brawler_hyper_charge alter column player_brawler_hyper_charge_id drop identity if exists;
alter table player_brawler_hyper_charge alter column player_brawler_hyper_charge_id set default nextval('player_brawler_hyper_charge_seq');
select setval('player_brawler_hyper_charge_seq', (select coalesce(max(player_brawler_hyper_charge_id), 0) + 100 from player_brawler_hyper_charge));

create sequence if not exists player_brawler_buffy_seq increment by 100 owned by player_brawler_buffy.player_brawler_buffy_id;
alter table player_brawler_buffy alter column player_brawler_buffy_id drop identity if exists;
alter table player_brawler_buffy alter column player_brawler_buffy_id set default nextval('player_brawler_buffy_seq');
select setval('player_brawler_buffy_seq', (select coalesce(max(player_brawler_buffy_id), 0) + 100 from player_brawler_buffy));

create sequence if not exists battle_seq increment by 100 owned by battle.battle_id;
alter table battle alter column battle_id drop identity if exists;
alter table battle alter column battle_id set default nextval('battle_seq');
select setval('battle_seq', (select coalesce(max(battle_id), 0) + 100 from battle));

create sequence if not exists battle_team_player_seq increment by 100 owned by battle_team_player.battle_team_player_id;
alter table battle_team_player alter column battle_team_player_id drop identity if exists;
alter table battle_team_player alter column battle_team_player_id set default nextval('battle_team_player_seq');
select setval('battle_team_player_seq', (select coalesce(max(battle_team_player_id), 0) + 100 from battle_team_player));
