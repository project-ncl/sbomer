import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { Button, EmptyState, EmptyStateBody, EmptyStateFooter, PageSection } from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useNavigate } from 'react-router-dom';
;

const NotFoundPage: React.FunctionComponent = () => {
  function GoHomeBtn() {
    const navigate = useNavigate();
    function handleClick() {
      navigate('/');
    }
    return <Button onClick={handleClick}>Take me home</Button>;
  }

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <EmptyState headingLevel="h1" icon={ExclamationTriangleIcon} titleText="404 Page not found" variant="full">
          <EmptyStateBody>We didn&apos;t find a page that matches the address you navigated to.</EmptyStateBody>
          <EmptyStateFooter>
            <GoHomeBtn />
          </EmptyStateFooter>
        </EmptyState>
      </PageSection>
    </AppLayout>
  );
};

export { NotFoundPage };
