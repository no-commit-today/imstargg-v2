-- 통계 집계 테이블 10종 PK 를 IDENTITY → SEQUENCE(increment 100) 로 전환한다.
-- Hibernate 는 IDENTITY 에서 JDBC insert 배칭을 비활성화한다. SEQUENCE(pooled, allocationSize=100)
-- 전환으로 core-batch 통계 쓰기의 INSERT 배칭이 활성화된다 (엔티티 @SequenceGenerator 와 일치 필수).
-- `set default nextval` 은 IDENTITY 를 기대하는 구버전 앱과의 호환 안전망이다 (DDL 선적용 → 앱 배포).
-- setval 은 max(id)+100 으로 초기화해 배포 전환 구간의 잔여 insert 와도 충돌하지 않는다.
-- 멱등: 재실행 시 setval 만 +100 재상승 (무해).

create sequence if not exists brawler_battle_result_stats_seq increment by 100 owned by brawler_battle_result_stats.brawler_battle_result_stats_id;
alter table brawler_battle_result_stats alter column brawler_battle_result_stats_id drop identity if exists;
alter table brawler_battle_result_stats alter column brawler_battle_result_stats_id set default nextval('brawler_battle_result_stats_seq');
select setval('brawler_battle_result_stats_seq', (select coalesce(max(brawler_battle_result_stats_id), 0) + 100 from brawler_battle_result_stats));

create sequence if not exists brawler_battle_rank_stats_seq increment by 100 owned by brawler_battle_rank_stats.brawler_battle_rank_stats_id;
alter table brawler_battle_rank_stats alter column brawler_battle_rank_stats_id drop identity if exists;
alter table brawler_battle_rank_stats alter column brawler_battle_rank_stats_id set default nextval('brawler_battle_rank_stats_seq');
select setval('brawler_battle_rank_stats_seq', (select coalesce(max(brawler_battle_rank_stats_id), 0) + 100 from brawler_battle_rank_stats));

create sequence if not exists brawler_pair_battle_result_stats_seq increment by 100 owned by brawler_pair_battle_result_stats.brawler_pair_battle_result_stats_id;
alter table brawler_pair_battle_result_stats alter column brawler_pair_battle_result_stats_id drop identity if exists;
alter table brawler_pair_battle_result_stats alter column brawler_pair_battle_result_stats_id set default nextval('brawler_pair_battle_result_stats_seq');
select setval('brawler_pair_battle_result_stats_seq', (select coalesce(max(brawler_pair_battle_result_stats_id), 0) + 100 from brawler_pair_battle_result_stats));

create sequence if not exists brawler_pair_battle_rank_stats_seq increment by 100 owned by brawler_pair_battle_rank_stats.brawler_pair_battle_rank_stats_id;
alter table brawler_pair_battle_rank_stats alter column brawler_pair_battle_rank_stats_id drop identity if exists;
alter table brawler_pair_battle_rank_stats alter column brawler_pair_battle_rank_stats_id set default nextval('brawler_pair_battle_rank_stats_seq');
select setval('brawler_pair_battle_rank_stats_seq', (select coalesce(max(brawler_pair_battle_rank_stats_id), 0) + 100 from brawler_pair_battle_rank_stats));

create sequence if not exists brawler_enemy_battle_result_stats_seq increment by 100 owned by brawler_enemy_battle_result_stats.brawler_enemy_battle_result_stats_id;
alter table brawler_enemy_battle_result_stats alter column brawler_enemy_battle_result_stats_id drop identity if exists;
alter table brawler_enemy_battle_result_stats alter column brawler_enemy_battle_result_stats_id set default nextval('brawler_enemy_battle_result_stats_seq');
select setval('brawler_enemy_battle_result_stats_seq', (select coalesce(max(brawler_enemy_battle_result_stats_id), 0) + 100 from brawler_enemy_battle_result_stats));

create sequence if not exists brawler_gear_ownership_stats_seq increment by 100 owned by brawler_gear_ownership_stats.brawler_gear_ownership_stats_id;
alter table brawler_gear_ownership_stats alter column brawler_gear_ownership_stats_id drop identity if exists;
alter table brawler_gear_ownership_stats alter column brawler_gear_ownership_stats_id set default nextval('brawler_gear_ownership_stats_seq');
select setval('brawler_gear_ownership_stats_seq', (select coalesce(max(brawler_gear_ownership_stats_id), 0) + 100 from brawler_gear_ownership_stats));

create sequence if not exists brawler_gadget_ownership_stats_seq increment by 100 owned by brawler_gadget_ownership_stats.brawler_gadget_ownership_stats_id;
alter table brawler_gadget_ownership_stats alter column brawler_gadget_ownership_stats_id drop identity if exists;
alter table brawler_gadget_ownership_stats alter column brawler_gadget_ownership_stats_id set default nextval('brawler_gadget_ownership_stats_seq');
select setval('brawler_gadget_ownership_stats_seq', (select coalesce(max(brawler_gadget_ownership_stats_id), 0) + 100 from brawler_gadget_ownership_stats));

create sequence if not exists brawler_star_power_ownership_stats_seq increment by 100 owned by brawler_star_power_ownership_stats.brawler_star_power_ownership_stats_id;
alter table brawler_star_power_ownership_stats alter column brawler_star_power_ownership_stats_id drop identity if exists;
alter table brawler_star_power_ownership_stats alter column brawler_star_power_ownership_stats_id set default nextval('brawler_star_power_ownership_stats_seq');
select setval('brawler_star_power_ownership_stats_seq', (select coalesce(max(brawler_star_power_ownership_stats_id), 0) + 100 from brawler_star_power_ownership_stats));

create sequence if not exists brawler_hyper_charge_ownership_stats_seq increment by 100 owned by brawler_hyper_charge_ownership_stats.brawler_hyper_charge_ownership_stats_id;
alter table brawler_hyper_charge_ownership_stats alter column brawler_hyper_charge_ownership_stats_id drop identity if exists;
alter table brawler_hyper_charge_ownership_stats alter column brawler_hyper_charge_ownership_stats_id set default nextval('brawler_hyper_charge_ownership_stats_seq');
select setval('brawler_hyper_charge_ownership_stats_seq', (select coalesce(max(brawler_hyper_charge_ownership_stats_id), 0) + 100 from brawler_hyper_charge_ownership_stats));

create sequence if not exists brawler_buffy_ownership_stats_seq increment by 100 owned by brawler_buffy_ownership_stats.brawler_buffy_ownership_stats_id;
alter table brawler_buffy_ownership_stats alter column brawler_buffy_ownership_stats_id drop identity if exists;
alter table brawler_buffy_ownership_stats alter column brawler_buffy_ownership_stats_id set default nextval('brawler_buffy_ownership_stats_seq');
select setval('brawler_buffy_ownership_stats_seq', (select coalesce(max(brawler_buffy_ownership_stats_id), 0) + 100 from brawler_buffy_ownership_stats));
