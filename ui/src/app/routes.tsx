import * as React from 'react';
import { Dashboard } from './components/Pages/Dashboard/Dashboard';
import { GenerationRequestPage } from './components/Pages/GenerationRequests/GenerationRequestPage';
import { GenerationRequestsPage } from './components/Pages/GenerationRequests/GenerationRequestsPage';
import { ManifestPage } from './components/Pages/Manifests/ManifestPage';
import { ManifestsPage } from './components/Pages/Manifests/ManifestsPage';
import { NotFoundPage } from './components/Pages/NotFound/NotFoundPage';

let routeFocusTimer: number;
export interface IAppRoute {
  label?: string; // Excluding the label will exclude the route from the nav sidebar in AppLayout
  element: React.ReactNode;
  path: string;
  routes?: undefined;
}

export interface IAppRouteGroup {
  label: string;
  routes: IAppRoute[];
}

export type AppRouteConfig = IAppRoute | IAppRouteGroup;

const routes: AppRouteConfig[] = [
  {
    element: <Dashboard />,
    label: 'Dashboard',
    path: '/',
  },
  {
    element: <GenerationRequestsPage />,
    path: '/requests',
  },
  {
    element: <GenerationRequestPage />,
    path: '/requests/:id',
  },
  {
    element: <GenerationRequestsPage />,
    label: 'Generations',
    path: '/generations',
  },
  {
    element: <GenerationRequestPage />,
    path: '/generations/:id',
  },
  {
    element: <ManifestsPage />,
    label: 'Manifests',
    path: '/manifests',
  },
  {
    element: <ManifestPage />,
    path: '/manifests/:id',
  },
  {
    element: <NotFoundPage />,
    path: '*',
  },
];

export { routes };
