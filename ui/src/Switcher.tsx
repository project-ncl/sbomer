import React, { useState } from "react";
import App from './app/index';
import AppV2 from './appV2/index';

const AppSwitcherWrapper = () => {
  const [useV2, setUseV2] = useState(true);

  return (
    <>
      <div style={{ padding: '1rem', background: '#f5f5f5' }}>
        <button onClick={() => setUseV2(!useV2)}>
          Switch to {useV2 ? 'App V1' : 'App V2'}
        </button>
      </div>
      {useV2 ? <AppV2 /> : <App />}
    </>
  );
};

export default AppSwitcherWrapper;
