import { GenerationRequestTable } from '@appV2/components/GenerationRequestTable/GenerationRequestTable';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid, Stack } from '@carbon/react';
import * as React from 'react';

export function GenerationRequestsPage() {
  useDocumentTitle('SBOMer | Generations');

  return (
    <AppLayout>
            <Stack gap={4}>
              <GenerationRequestTable />
            </Stack>
    </AppLayout>
  );
}
