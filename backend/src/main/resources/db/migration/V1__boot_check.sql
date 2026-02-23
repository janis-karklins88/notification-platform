create table if not exists _boot_check (
  id bigserial primary key,
  created_at timestamptz not null default now()
);

insert into _boot_check default values;