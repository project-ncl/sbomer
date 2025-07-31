import React from "react";
import App from './app/index';
import AppV2 from './appV2/index';

import '@carbon/styles/css/styles.css';
import './appV2/styles.scss';


const AppSwitcherWrapper = () => {
  const useV2 = window.location.pathname.startsWith('/nextgen');

  return (
    <>
      {useV2 ? (
        <div className="carbon-styles-scope">
          <AppV2 basename="/nextgen" />
        </div>
      ) : (
        <div className="patternfly-app">
          <App />
        </div>
      )}
    </>
  );
};

export default AppSwitcherWrapper;
