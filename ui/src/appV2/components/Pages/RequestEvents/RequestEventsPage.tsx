import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { RequestEventTable } from '@appV2/components/RequestEventTable/RequestEventTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';

export function RequestEventsPage() {
  useDocumentTitle('SBOMer | Request Events');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <Title headingLevel="h1" size="4xl">
              Events
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
