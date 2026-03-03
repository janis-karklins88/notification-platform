create table delivery (
  id uuid primary key,
  tenant_id uuid not null,
  event_id uuid not null,
  subscription_id uuid not null,
  endpoint_id uuid not null,
  status varchar(32) not null,
  attempt_count integer not null default 0,
  next_attempt_at timestamptz null,
  last_attempt_at timestamptz null,
  delivered_at timestamptz null,
  failed_at timestamptz null,
  last_error text null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_delivery_tenant_id foreign key (tenant_id) references tenant (id),
  constraint fk_delivery_event_id foreign key (event_id) references event (id),
  constraint fk_delivery_subscription_id foreign key (subscription_id) references subscription (id),
  constraint fk_delivery_endpoint_id foreign key (endpoint_id) references endpoint (id),
  constraint uk_delivery_tenant_event_subscription unique (tenant_id, event_id, subscription_id),
  constraint ck_delivery_status check (status in ('PENDING', 'IN_PROGRESS', 'RETRY_SCHEDULED', 'DELIVERED', 'FAILED'))
);

create index idx_delivery_tenant_status
  on delivery (tenant_id, status);
create index idx_delivery_status_next_attempt_at
  on delivery (status, next_attempt_at);
create index idx_delivery_event_id
  on delivery (event_id);
