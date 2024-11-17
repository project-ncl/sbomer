import { Button, EmptyState, EmptyStateBody, EmptyStateFooter, PageSection } from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useHistory } from 'react-router-dom';

const NotFoundPage: React.FunctionComponent = () => {
  function GoHomeBtn() {
    const history = useHistory();
    function handleClick() {
      history.push('/');
    }
    return <Button onClick={handleClick}>Take me home</Button>;
  }

  return (
    <PageSection hasBodyWrapper={false}>
      <EmptyState headingLevel="h1" icon={ExclamationTriangleIcon} titleText="404 Page not found" variant="full">
        <EmptyStateBody>We didn&apos;t find a page that matches the address you navigated to.</EmptyStateBody>
        <EmptyStateFooter>
          <GoHomeBtn />
        </EmptyStateFooter>
      </EmptyState>
    </PageSection>
  );
};

export { NotFoundPage };
