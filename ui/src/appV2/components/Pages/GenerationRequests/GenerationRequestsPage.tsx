import { GenerationRequestTable } from '@appV2/components/GenerationRequestTable/GenerationRequestTable';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid } from '@carbon/react';
import * as React from 'react';

export function GenerationRequestsPage() {
  useDocumentTitle('SBOMer | Generations');

  return (
    <AppLayout>
        <Grid>
          <Column span={6}>
            <h1>Generations</h1>
          </Column>
          <Column span={12}>
            <GenerationRequestTable />
          </Column>
        </Grid>
    </AppLayout>
  );
}
