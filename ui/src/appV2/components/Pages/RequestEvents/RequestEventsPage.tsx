import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { RequestEventTable } from '@appV2/components/RequestEventTable/RequestEventTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Stack } from '@carbon/react';
import * as React from 'react';

export function RequestEventsPage() {
  useDocumentTitle('SBOMer | Events');

  return (
    <AppLayout>
      <Stack gap={4}>
        <h1>Events</h1>
        <RequestEventTable />
      </Stack>
    </AppLayout>
  );
}
