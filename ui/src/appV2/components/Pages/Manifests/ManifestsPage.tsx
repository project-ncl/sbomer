import { ManifestsTable } from '@appV2/components/ManifestsTableTable/ManifestsTable';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';

const ManifestsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Manifests');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
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
    </AppLayout>
  );
};

export { ManifestsPage };
