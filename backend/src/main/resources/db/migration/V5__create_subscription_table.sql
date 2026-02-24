create table subscription (
  id uuid primary key,
  tenant_id uuid not null,
  event_type varchar(150) not null,
  endpoint_id uuid not null,
  status varchar(32) not null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_subscription_tenant_id foreign key (tenant_id) references tenant (id),
  constraint fk_subscription_endpoint_id foreign key (endpoint_id) references endpoint (id),
  constraint uk_subscription_tenant_event_endpoint unique (tenant_id, event_type, endpoint_id),
  constraint ck_subscription_status check (status in ('ACTIVE', 'PAUSED'))
);

create index idx_subscription_tenant_event_status
  on subscription (tenant_id, event_type, status);
create index idx_subscription_endpoint_id
  on subscription (endpoint_id);
