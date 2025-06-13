import React from "react";
import App from './app/index';
import AppV2 from './appV2/index';


const AppSwitcherWrapper = () => {
  const useV2 = window.location.pathname.startsWith('/nextgen');

  return (
    <>
      {useV2 ? <AppV2 basename="/nextgen"/> : <App />}
    </>
  );
};

export default AppSwitcherWrapper;
