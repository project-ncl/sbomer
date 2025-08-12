import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { Button, Content, Stack } from '@carbon/react';
import { ErrorFilled } from '@carbon/icons-react';
import * as React from 'react';
import { useNavigate } from 'react-router-dom';

const NotFoundPage: React.FunctionComponent = () => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <AppLayout>
      <Content>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          textAlign: 'center'
        }}>
          <Stack gap={7}>
            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <ErrorFilled size={64} style={{ color: '#da1e28' }} />
            </div>
            <Stack gap={4}>
              <h1 style={{
                fontSize: '2.5rem',
                fontWeight: 400,
                margin: 0,
                color: '#161616'
              }}>
                404 - Page not found
              </h1>
              <p style={{
                fontSize: '1rem',
                color: '#525252',
                maxWidth: '400px',
                margin: '0 auto'
              }}>
                We couldn't find the page you're looking for. The page may have been moved, deleted, or the URL might be incorrect.
              </p>
            </Stack>
            <div>
              <Button
                kind="primary"
                onClick={handleGoHome}
                size="lg"
              >
                Take me home
              </Button>
            </div>
          </Stack>
        </div>
      </Content>
    </AppLayout>
  );
};

export { NotFoundPage };
