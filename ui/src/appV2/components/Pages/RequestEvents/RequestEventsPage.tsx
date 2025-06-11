import { RequestEventTable } from '@app/components/RequestEventTable/RequestEventTable';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';

export function RequestEventsPage() {
  useDocumentTitle('SBOMer | Request Events');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <Title headingLevel="h1" size="4xl">
              Request Events
            </Title>
          </GridItem>
          <GridItem span={12}>
            <RequestEventTable />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
}
