create index if not exists idx_event_status_received_at
  on event (status, received_at);

create index if not exists idx_subscription_tenant_status
  on subscription (tenant_id, status);

create index if not exists idx_endpoint_tenant_status
  on endpoint (tenant_id, status);
