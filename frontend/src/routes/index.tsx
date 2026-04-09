import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AppLayout } from '../layouts/AppLayout'
import { ApiKeysPage } from '../pages/ApiKeysPage'
import { DeliveryMonitoringPage } from '../pages/DeliveryMonitoringPage'
import { EmailTemplatesPage } from '../pages/EmailTemplatesPage'
import { EndpointsPage } from '../pages/EndpointsPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { SubscriptionsPage } from '../pages/SubscriptionsPage'
import { TenantOverviewPage } from '../pages/TenantOverviewPage'
import { TenantsPage } from '../pages/TenantsPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate replace to="/tenants" /> },
      { path: 'tenants', element: <TenantsPage /> },
      { path: 'tenants/endpoints', element: <EndpointsPage /> },
      { path: 'tenants/subscriptions', element: <SubscriptionsPage /> },
      { path: 'tenants/api-keys', element: <ApiKeysPage /> },
      { path: 'tenants/email-templates', element: <EmailTemplatesPage /> },
      { path: 'tenants/:tenantId', element: <TenantOverviewPage /> },
      { path: 'tenants/:tenantId/endpoints', element: <EndpointsPage /> },
      { path: 'tenants/:tenantId/subscriptions', element: <SubscriptionsPage /> },
      { path: 'tenants/:tenantId/api-keys', element: <ApiKeysPage /> },
      { path: 'tenants/:tenantId/email-templates', element: <EmailTemplatesPage /> },
      { path: 'endpoints', element: <EndpointsPage /> },
      { path: 'subscriptions', element: <SubscriptionsPage /> },
      { path: 'api-keys', element: <ApiKeysPage /> },
      { path: 'apikeys', element: <ApiKeysPage /> },
      { path: 'deliveries', element: <DeliveryMonitoringPage /> },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
