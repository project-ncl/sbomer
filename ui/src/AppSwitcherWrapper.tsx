import React, { useState } from "react";
import App from './app/index';
import AppV2 from './appV2/index';
import { Button } from '@patternfly/react-core';

const AppSwitcherWrapper = () => {
  const [useV2, setUseV2] = useState(true);

  return (
    <>
      <div
        style={{
          position: 'fixed',
          top: '1rem',
          right: '1rem',
          zIndex: 1100,
          padding: '0.5rem 1rem',
          backgroundColor: '#f0f0f0',
          border: '1px solid #d2d2d2',
          borderRadius: '8px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}
      >
        <span style={{ fontSize: '0.9rem', color: '#333', fontWeight: 'bold' }}>
          {useV2 ? 'App V2' : 'App V1'}
        </span>
        <Button
          variant="primary"
          size="sm"
          onClick={() => setUseV2(!useV2)}
          style={{
            minWidth: '100px',
          }}
        >
          Switch to {useV2 ? 'App V1' : 'App V2'}
        </Button>
      </div>
      {useV2 ? <AppV2 /> : <App />}
    </>
  );
};

export default AppSwitcherWrapper;
