import { RequestEventDetailsTable } from '@app/components/RequestEventTable/RequestEventDetailsTable';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';

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
