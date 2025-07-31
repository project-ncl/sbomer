import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { RequestEventTable } from '@appV2/components/RequestEventTable/RequestEventTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid } from '@carbon/react';
import * as React from 'react';

export function RequestEventsPage() {
  useDocumentTitle('SBOMer | Request Events');

  return (
    <AppLayout>
      <Grid>
          <Column span={6}>
            <h1>Events</h1>
          </Column>
          <Column span={12}>
            <RequestEventTable />
          </Column>
        </Grid>
    </AppLayout>
  );
}
