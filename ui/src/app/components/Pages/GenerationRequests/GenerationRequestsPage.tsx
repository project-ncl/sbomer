import { GenerationRequestTable } from '@app/components/GenerationRequestTable/GenerationRequestTable';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';

const GenerationRequestsPage: React.FunctionComponent = () => {
  return (
    <PageSection>
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Generation Requests
          </Title>
        </GridItem>
        <GridItem span={12}>
          <GenerationRequestTable />
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { GenerationRequestsPage };
