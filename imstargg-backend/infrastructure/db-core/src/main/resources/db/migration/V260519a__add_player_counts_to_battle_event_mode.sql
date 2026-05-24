alter table battle_event_mode
    add column if not exists player_counts jsonb not null default '[3, 3]'::jsonb;
