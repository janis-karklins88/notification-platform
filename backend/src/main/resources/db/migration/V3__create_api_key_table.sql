create table api_key (
  id uuid primary key,
  tenant_id uuid not null,
  key_prefix varchar(64) not null,
  key_hash varchar(128) not null,
  status varchar(32) not null,
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  revoked_at timestamptz null,
  last_used_at timestamptz null,
  constraint uk_api_key_key_hash unique (key_hash),
  constraint fk_api_key_tenant_id foreign key (tenant_id) references tenant (id),
  constraint ck_api_key_status check (status in ('ACTIVE', 'INACTIVE', 'REVOKED'))
);

create index idx_api_key_tenant_id on api_key (tenant_id);
create index idx_api_key_status on api_key (status);
