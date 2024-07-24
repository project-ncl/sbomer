import { Dashboard } from '@app/components/Pages/Dashboard/Dashboard';
import { NotFoundPage } from '@app/components/Pages/NotFound/NotFoundPage';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';
import * as React from 'react';
import { Route, RouteComponentProps, Switch, useLocation } from 'react-router-dom';
import { GenerationRequestPage } from './components/Pages/GenerationRequests/GenerationRequestPage';
import { GenerationRequestsPage } from './components/Pages/GenerationRequests/GenerationRequestsPage';
import { ManifestsPage } from './components/Pages/Manifests/ManifestsPage';
import { ManifestPage } from './components/Pages/Manifests/ManifestPage';

let routeFocusTimer: number;
export interface IAppRoute {
  label?: string; // Excluding the label will exclude the route from the nav sidebar in AppLayout
  /* eslint-disable @typescript-eslint/no-explicit-any */
  component: React.ComponentType<RouteComponentProps<any>> | React.ComponentType<any>;
  /* eslint-enable @typescript-eslint/no-explicit-any */
  exact?: boolean;
  path: string;
  title: string;
  routes?: undefined;
}

export interface IAppRouteGroup {
  label: string;
  routes: IAppRoute[];
}

export type AppRouteConfig = IAppRoute | IAppRouteGroup;

const routes: AppRouteConfig[] = [
  {
    component: Dashboard,
    exact: true,
    label: 'Dashboard',
    path: '/',
    title: 'SBOMer | Dashboard',
  },
  // {
  //   component: EmptyState,
  //   exact: true,
  //   label: 'Manifests',
  //   path: '/manifests',
  //   title: 'SBOMer | Manifess',
  // },
  {
    component: GenerationRequestsPage,
    exact: true,
    label: 'Generation Requests',
    path: '/requests',
    title: 'SBOMer | Generation Requests',
  },
  {
    component: GenerationRequestPage,
    exact: false,
    path: '/requests/:id',
    title: 'SBOMer | Generation Request',
  },
  {
    component: ManifestsPage,
    exact: true,
    label: 'Manifests',
    path: '/manifests',
    title: 'SBOMer | Manifests',
  },
  {
    component: ManifestPage,
    exact: false,
    path: '/manifests/:id',
    title: 'SBOMer | Manifest',
  },
  // {
  //   component: Support,
  //   exact: true,
  //   label: 'Support',
  //   path: '/support',
  //   title: 'SBOMer | Support',
  // },
];

// a custom hook for sending focus to the primary content container
// after a view has loaded so that subsequent press of tab key
// sends focus directly to relevant content
// may not be necessary if https://github.com/ReactTraining/react-router/issues/5210 is resolved
const useA11yRouteChange = () => {
  const { pathname } = useLocation();
  React.useEffect(() => {
    routeFocusTimer = window.setTimeout(() => {
      const mainContainer = document.getElementById('primary-app-container');
      if (mainContainer) {
        mainContainer.focus();
      }
    }, 50);
    return () => {
      window.clearTimeout(routeFocusTimer);
    };
  }, [pathname]);
};

const RouteWithTitleUpdates = ({ component: Component, title, ...rest }: IAppRoute) => {
  useA11yRouteChange();
  useDocumentTitle(title);

  function routeWithTitle(routeProps: RouteComponentProps) {
    return <Component {...rest} {...routeProps} />;
  }

  return <Route render={routeWithTitle} {...rest} />;
};

const PageNotFound = ({ title }: { title: string }) => {
  useDocumentTitle(title);
  return <Route component={NotFoundPage} />;
};

const flattenedRoutes: IAppRoute[] = routes.reduce(
  (flattened, route) => [...flattened, ...(route.routes ? route.routes : [route])],
  [] as IAppRoute[],
);

const AppRoutes = (): React.ReactElement => (
  <Switch>
    {flattenedRoutes.map(({ path, exact, component, title }, idx) => (
      <RouteWithTitleUpdates path={path} exact={exact} component={component} key={idx} title={title} />
    ))}
    <PageNotFound title="404 Page Not Found" />
  </Switch>
);

export { AppRoutes, routes };
