create table tenant (
  id uuid primary key,
  slug varchar(64) not null,
  name varchar(200) not null,
  status varchar(32) not null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_tenant_slug unique (slug),
  constraint ck_tenant_status check (status in ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

create index idx_tenant_status on tenant (status);
