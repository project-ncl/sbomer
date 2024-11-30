import { RequestEventManifestsTable } from '@app/components/RequestEventTable/RequestEventManifestsTable';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';

const RequestEventManifestsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Manifests Of Request');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <RequestEventManifestsTable />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
};

export { RequestEventManifestsPage };
