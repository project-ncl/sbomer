import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { ManifestPageContent } from '@appV2/components/Pages/Manifests/ManifestPageContent';
import * as React from 'react';

export function ManifestPage() {
  return (
    <AppLayout>
      <ManifestPageContent />
    </AppLayout>
  );
}
