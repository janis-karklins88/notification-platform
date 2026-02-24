create table event (
  id uuid primary key,
  tenant_id uuid not null,
  event_type varchar(150) not null,
  idempotency_key varchar(128) null,
  payload jsonb not null,
  status varchar(32) not null,
  version bigint not null default 0,
  received_at timestamptz not null default now(),
  source varchar(100) null,
  trace_id varchar(100) null,
  updated_at timestamptz not null default now(),
  constraint fk_event_tenant_id foreign key (tenant_id) references tenant (id),
  constraint uk_event_tenant_idempotency_key unique (tenant_id, idempotency_key),
  constraint ck_event_status check (status in ('RECEIVED', 'ROUTED', 'FAILED'))
);

create index idx_event_tenant_received_at
  on event (tenant_id, received_at);
create index idx_event_tenant_status
  on event (tenant_id, status);
create index idx_event_tenant_event_type
  on event (tenant_id, event_type);
