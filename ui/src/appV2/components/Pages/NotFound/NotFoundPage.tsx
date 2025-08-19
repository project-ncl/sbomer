import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { Button, Content, Stack, Heading } from '@carbon/react';
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
        <Stack gap={7}>
          <ErrorFilled size={64} />
          <Stack gap={4}>
            <Heading>
              404 - Page not found
            </Heading>
            <p>
              We couldn't find the page you're looking for. The page may have been moved, deleted, or the URL might be incorrect.
            </p>
          </Stack>
          <Button
            kind="primary"
            onClick={handleGoHome}
            size="lg"
          >
            Take me home
          </Button>
        </Stack>
      </Content>
    </AppLayout>
  );
};

export { NotFoundPage };
