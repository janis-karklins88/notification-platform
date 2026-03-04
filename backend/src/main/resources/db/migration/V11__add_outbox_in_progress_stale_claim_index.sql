create index if not exists idx_outbox_in_progress_last_attempt_at
  on outbox_event (last_attempt_at)
  where status = 'IN_PROGRESS';
