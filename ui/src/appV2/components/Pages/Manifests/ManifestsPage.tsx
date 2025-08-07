import { ManifestsTable } from '@appV2/components/ManifestsTable/ManifestsTable';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Stack } from '@carbon/react';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';

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
