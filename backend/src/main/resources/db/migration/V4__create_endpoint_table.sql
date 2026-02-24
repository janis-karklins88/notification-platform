create table endpoint (
  id uuid primary key,
  tenant_id uuid not null,
  type varchar(32) not null,
  url varchar(255) null,
  status varchar(32) not null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_endpoint_tenant_id foreign key (tenant_id) references tenant (id),
  constraint ck_endpoint_type check (type in ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'WEBHOOK')),
  constraint ck_endpoint_status check (status in ('ACTIVE', 'INACTIVE', 'DELETED'))
);

create index idx_endpoint_tenant_id on endpoint (tenant_id);
create index idx_endpoint_status on endpoint (status);
