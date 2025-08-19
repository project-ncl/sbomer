import { GenerationTable } from '@appV2/components/Tables/GenerationTable/GenerationTable';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid, Stack } from '@carbon/react';
import * as React from 'react';

export function GenerationsPage() {
  useDocumentTitle('SBOMer | Generations');

  return (
    <AppLayout>
            <Stack gap={4}>
              <GenerationTable />
            </Stack>
    </AppLayout>
  );
}
