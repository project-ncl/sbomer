import React, { useState } from "react";
import App from './app/index';
import AppV2 from './appV2/index';
import { Button } from '@patternfly/react-core';

const AppSwitcherWrapper = () => {
  const useV2 = window.location.pathname.startsWith('/nextgen');

  return (
    <>
      {useV2 ? <AppV2 basename="/nextgen"/> : <App />}
    </>
  );
};

export default AppSwitcherWrapper;
