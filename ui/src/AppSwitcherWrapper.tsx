import React, { Suspense, lazy } from 'react';

const App = lazy(() => import('./app/index'));
const AppV2 = lazy(() => import('./appV2/index'));

const LoadingScreen = () => <div>Loading...</div>;

const AppSwitcherWrapper = () => {
  const useV2 = window.location.pathname.startsWith('/nextgen');

  return (
    <Suspense fallback={<LoadingScreen />}>
      {useV2 ? (
          <AppV2 basename="/nextgen" />
      ) : (
        <App />
      )}
    </Suspense>
  );
};

export default AppSwitcherWrapper;
