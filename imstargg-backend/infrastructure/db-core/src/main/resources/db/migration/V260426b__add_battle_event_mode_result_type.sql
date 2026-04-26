-- battle_event_mode 에 결과 타입 컬럼 추가 (RESULT: 승/패/무, RANK: 1위~N위)
alter table battle_event_mode
    add column if not exists result_type varchar(20) not null default 'RESULT';
