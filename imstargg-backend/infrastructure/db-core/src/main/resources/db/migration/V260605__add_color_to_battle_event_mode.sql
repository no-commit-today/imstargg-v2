alter table battle_event_mode
    add column if not exists color varchar(7) not null default '#2A2E38';

-- 모드 대표색 시딩 (brawlstars_id 기준). 변형 모드는 베이스 모드 색을 상속한다.
-- 미지정 모드는 default '#2A2E38'(회색)으로 남으며 관리자 화면에서 지정한다.
update battle_event_mode set color = '#8F43EA' where brawlstars_id in (0, 33, 46, 65);    -- GEM GRAB 계열
update battle_event_mode set color = '#C763CD' where brawlstars_id = 2;                    -- HEIST
update battle_event_mode set color = '#FFC60A' where brawlstars_id = 3;                    -- BOUNTY
update battle_event_mode set color = '#909FDB' where brawlstars_id in (5, 32, 49);         -- BRAWL BALL 계열
update battle_event_mode set color = '#95D449' where brawlstars_id in (6, 9, 38, 67, 72, 78); -- SHOWDOWN 계열
update battle_event_mode set color = '#B83BFF' where brawlstars_id = 7;                    -- BIG GAME
update battle_event_mode set color = '#58D7E8' where brawlstars_id = 8;                    -- ROBO RUMBLE
update battle_event_mode set color = '#C9141A' where brawlstars_id = 10;                   -- BOSS FIGHT
update battle_event_mode set color = '#FF8A1F' where brawlstars_id in (17, 51);            -- HOT ZONE 계열
update battle_event_mode set color = '#E8893A' where brawlstars_id in (20, 35, 50);        -- KNOCKOUT 계열
update battle_event_mode set color = '#62C1F4' where brawlstars_id in (22, 52);            -- BASKET BRAWL 계열
update battle_event_mode set color = '#3DE0FF' where brawlstars_id = 23;                   -- VOLLEY BRAWL
update battle_event_mode set color = '#FF5DAA' where brawlstars_id = 24;                   -- DUELS
update battle_event_mode set color = '#FF3A3A' where brawlstars_id in (25, 31, 66);        -- WIPEOUT 계열
update battle_event_mode set color = '#D2D2FB' where brawlstars_id in (45, 53, 57);        -- BRAWL HOCKEY 계열
