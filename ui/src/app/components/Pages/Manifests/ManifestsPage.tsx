import { ManifestsTable } from '@app/components/ManifestsTableTable/ManifestsTable';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';

const ManifestsPage: React.FunctionComponent = () => {
  return (
    <PageSection>
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Manifests
          </Title>
        </GridItem>
        <GridItem span={12}>
          <ManifestsTable />
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { ManifestsPage };
