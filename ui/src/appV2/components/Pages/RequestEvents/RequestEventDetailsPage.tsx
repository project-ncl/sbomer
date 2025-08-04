import { RequestEventDetailsTable } from '@appV2/components/RequestEventTable/RequestEventDetailsTable';
import { Content, Grid, Column } from '@carbon/react';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';

const RequestEventDetailsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Request Event details');

  return (
    <AppLayout>
      <Content>
        <Grid>
          <Column span={16}>
            <RequestEventDetailsTable />
          </Column>
        </Grid>
      </Content>
    </AppLayout>
  );
};

export { RequestEventDetailsPage };
