import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { EventDetailsTable } from '@appV2/components/Tables/EventTable/EventDetailsTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import * as React from 'react';

const EventDetailsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Event details');

  return (
    <AppLayout>
      <EventDetailsTable />
    </AppLayout>
  );
};

export { EventDetailsPage };
