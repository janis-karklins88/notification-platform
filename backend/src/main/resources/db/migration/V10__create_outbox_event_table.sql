create table outbox_event (
  id uuid primary key,
  tenant_id uuid not null,
  aggregate_type varchar(100) not null,
  aggregate_id uuid not null,
  event_type varchar(100) not null,
  payload jsonb not null,
  status varchar(32) not null,
  attempt_count integer not null default 0,
  available_at timestamptz not null default now(),
  last_attempt_at timestamptz null,
  published_at timestamptz null,
  last_error text null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_outbox_event_tenant_id foreign key (tenant_id) references tenant (id),
  constraint uk_outbox_tenant_aggregate_event unique (tenant_id, aggregate_type, aggregate_id, event_type),
  constraint ck_outbox_status check (status in ('PENDING', 'IN_PROGRESS', 'PUBLISHED', 'FAILED'))
);

create index idx_outbox_status_available_at
  on outbox_event (status, available_at);
create index idx_outbox_tenant_status
  on outbox_event (tenant_id, status);
create index idx_outbox_aggregate
  on outbox_event (aggregate_type, aggregate_id);
