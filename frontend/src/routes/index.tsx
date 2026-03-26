import { createBrowserRouter } from 'react-router-dom'

import { AppLayout } from '../layouts/AppLayout'
import { DeliveryMonitoringPage } from '../pages/DeliveryMonitoringPage'
import { DashboardPage } from '../pages/DashboardPage'
import { EndpointsPage } from '../pages/EndpointsPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { SubscriptionsPage } from '../pages/SubscriptionsPage'
import { TenantsPage } from '../pages/TenantsPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'tenants', element: <TenantsPage /> },
      { path: 'endpoints', element: <EndpointsPage /> },
      { path: 'subscriptions', element: <SubscriptionsPage /> },
      { path: 'deliveries', element: <DeliveryMonitoringPage /> },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
