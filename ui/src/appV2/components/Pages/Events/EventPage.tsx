import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { EventPageContent } from '@appV2/components/Pages/Events/EventPageContent';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import * as React from 'react';

const EventPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Event details');

  return (
    <AppLayout>
      <EventPageContent />
    </AppLayout>
  );
};

export { EventPage };
