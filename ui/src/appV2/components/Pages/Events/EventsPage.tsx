import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { EventTable } from '@appV2/components/Tables/EventTable/EventTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Stack } from '@carbon/react';
import * as React from 'react';

export function EventsPage() {
  useDocumentTitle('SBOMer | Events');

  return (
    <AppLayout>
        <EventTable />
    </AppLayout>
  );
}
