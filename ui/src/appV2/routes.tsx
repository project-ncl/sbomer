import * as React from 'react';
import { Dashboard } from './components/Pages/Dashboard/Dashboard';
import { GenerationPage } from './components/Pages/Generations/GenerationPage';
import { GenerationsPage } from './components/Pages/Generations/GenerationsPage';
import { ManifestPage } from './components/Pages/Manifests/ManifestPage';
import { ManifestsPage } from './components/Pages/Manifests/ManifestsPage';
import { EventsPage } from './components/Pages/Events/EventsPage';
import { EventDetailsPage } from './components/Pages/Events/EventDetailsPage';
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
    element: <GenerationsPage />,
    path: '/requests',
  },
  {
    element: <GenerationPage />,
    path: '/requests/:id',
  },
  {
    element: <GenerationsPage />,
    label: 'Generations',
    path: '/generations',
  },
  {
    element: <GenerationPage />,
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
    element: <EventsPage />,
    label: 'Events',
    path: '/events',
  },
  {
    element: <EventDetailsPage />,
    path: '/events/:id',
  },
  {
    element: <NotFoundPage />,
    path: '*',
  },
];

export { routes };
