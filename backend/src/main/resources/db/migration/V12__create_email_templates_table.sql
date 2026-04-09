create table email_templates (
  id uuid primary key,
  tenant_id uuid not null,
  name varchar(100) not null,
  subject varchar(255) not null,
  body text not null,
  is_html boolean not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  description varchar(500),
  constraint fk_email_templates_tenant_id foreign key (tenant_id) references tenant (id),
  constraint uk_template_name_tenant unique (tenant_id, name)
);

create index idx_email_templates_tenant_id
  on email_templates (tenant_id);
