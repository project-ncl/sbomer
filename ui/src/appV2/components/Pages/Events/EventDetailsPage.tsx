import { EventDetailsTable } from '@appV2/components/EventTable/EventDetailsTable';
import { Content, Grid, Column } from '@carbon/react';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';

const EventDetailsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Event details');

  return (
    <AppLayout>
      <Content>
        <Grid>
          <Column span={16}>
            <EventDetailsTable />
          </Column>
        </Grid>
      </Content>
    </AppLayout>
  );
};

export { EventDetailsPage };
