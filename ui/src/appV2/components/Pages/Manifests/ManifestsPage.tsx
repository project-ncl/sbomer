import { ManifestsTable } from '@appV2/components/ManifestsTableTable/ManifestsTable';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid } from '@carbon/react';

const ManifestsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Manifests');

  return (
    <AppLayout>
        <Grid>
          <Column span={6}>
            <h1>Manifests</h1>
          </Column>
          <Column span={12}>
            <ManifestsTable />
          </Column>
        </Grid>
    </AppLayout>
  );
};

export { ManifestsPage };
