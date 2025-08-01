import * as React from 'react';
import { createBrowserRouter, RouteObject, RouterProvider } from 'react-router-dom';

import { IAppRoute, routes } from './routes';
import './carbon-styles.scss';
import { Theme } from '@carbon/react';

const AppV2 = ({ basename }: { basename: string }) => {
  return (
      <Theme theme="g10">
        <RouterProvider
          router={createBrowserRouter(
            routes
              .filter((route) => !route.routes)
              .map((route: IAppRoute) => ({ element: route.element, path: route.path }) as RouteObject),
            { basename: basename }
          )}
        />
      </Theme>
  );
};

export default AppV2;
