import * as React from 'react';
import { Dashboard } from './components/Pages/Dashboard/Dashboard';
import { GenerationPage } from './components/Pages/Generations/GenerationPage';
import { GenerationsPage } from './components/Pages/Generations/GenerationsPage';
import { ManifestPage } from './components/Pages/Manifests/ManifestPage';
import { ManifestsPage } from './components/Pages/Manifests/ManifestsPage';
import { EventsPage } from './components/Pages/Events/EventsPage';
import { EventPage } from './components/Pages/Events/EventPage';
import { NotFoundPage } from './components/Pages/NotFound/NotFoundPage';
import { HelpPage } from './components/Pages/Help/HelpPage';

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
    element: <EventPage />,
    path: '/events/:id',
  },
  {
    element: <HelpPage />,
    label: 'Help',
    path: '/help',
  },
  {
    element: <NotFoundPage />,
    path: '*',
  },
];

export { routes };
