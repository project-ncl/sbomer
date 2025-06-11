import '@appV2/app.css';
import '@patternfly/react-core/dist/styles/base.css';
import * as React from 'react';
import { createBrowserRouter, RouteObject, RouterProvider } from 'react-router-dom';

import { IAppRoute, routes } from './routes';

const AppV2 = () => {
  return (
    <RouterProvider
      router={createBrowserRouter(
        routes
          .filter((route) => !route.routes)
          .map((route: IAppRoute) => ({ element: route.element, path: route.path }) as RouteObject),
      )}
    />
  );
};

export default AppV2;
