update endpoint
set status = 'DISABLED'
where status = 'DELETED';

alter table endpoint
  drop constraint ck_endpoint_status;

alter table endpoint
  add constraint ck_endpoint_status
    check (status in ('ACTIVE', 'INACTIVE', 'DISABLED'));

alter table endpoint
  add column config jsonb;

update endpoint
set config = case
  when url is not null and btrim(url) <> '' then jsonb_build_object('url', url)
  else '{}'::jsonb
end
where config is null;

alter table endpoint
  alter column config set not null;

alter table endpoint
  drop column url;

alter table endpoint
  add constraint uk_endpoint_tenant_id_id unique (tenant_id, id);

alter table subscription
  drop constraint ck_subscription_status;

alter table subscription
  add constraint ck_subscription_status
    check (status in ('ACTIVE', 'PAUSED', 'DELETED'));

alter table subscription
  add constraint fk_subscription_tenant_endpoint
    foreign key (tenant_id, endpoint_id) references endpoint (tenant_id, id);
