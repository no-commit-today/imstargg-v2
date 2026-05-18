-- V260519__rename_feedback_indexes_to_ix_prefix.sql
alter index if exists idx_feedback_created_at rename to ix_feedback__createdat;
alter index if exists idx_feedback_checked    rename to ix_feedback__checked;
