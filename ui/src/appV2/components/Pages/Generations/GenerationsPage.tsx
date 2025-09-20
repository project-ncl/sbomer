import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { GenerationTable } from '@appV2/components/Tables/GenerationTable/GenerationTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Stack } from '@carbon/react';
import * as React from 'react';

export function GenerationsPage() {
  useDocumentTitle('SBOMer | Generations');

  return (
    <AppLayout>
      <GenerationTable />
    </AppLayout>
  );
}
