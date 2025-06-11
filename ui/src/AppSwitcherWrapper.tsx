import React, { useState } from "react";
import App from './app/index';
import AppV2 from './appV2/index';
import { Alert, Button } from '@patternfly/react-core';

const AppSwitcherWrapper = () => {
  const [useV2, setUseV2] = useState(true);

  return (
    <>
      <div
        style={{
          position: 'fixed',
          top: 0,
          right: 0,
          zIndex: 1100,
          margin: '1rem',
          minWidth: 'auto',
          maxWidth: '300px',
          border: '2px solid #0066cc',
          borderRadius: '6px',
          background: '#fff'
        }}
      >
        <Alert
          variant="info"
          isInline
          isPlain
          title={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
              <span style={{ fontSize: '0.85rem', marginRight: 8 }}>
                {useV2 ? 'App V2' : 'App V1'}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setUseV2(!useV2)}
              >
                Switch to {useV2 ? 'App V1' : 'App V2'}
              </Button>
            </div>
          }
          style={{
            marginBottom: 0,
            padding: '0.25rem 0.75rem',
            background: 'transparent'
          }}
        />
      </div>
      {useV2 ? <AppV2 /> : <App />}
    </>
  );
};

export default AppSwitcherWrapper;
