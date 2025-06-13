import { RequestEventDetailsTable } from '@appV2/components/RequestEventTable/RequestEventDetailsTable';
import { Grid, GridItem, PageSection } from '@patternfly/react-core';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';

const RequestEventDetailsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Request Event details');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <RequestEventDetailsTable />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
};

export { RequestEventDetailsPage };
