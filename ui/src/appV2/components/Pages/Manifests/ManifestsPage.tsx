import { ManifestsTable } from '@appV2/components/ManifestsTableTable/ManifestsTable';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Column, Grid, Stack } from '@carbon/react';

const ManifestsPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Manifests');

  return (
    <AppLayout>
      <Stack gap={4}>
      <h1>Manifests</h1>
      <ManifestsTable />
      </Stack>
    </AppLayout>
  );
};

export { ManifestsPage };
