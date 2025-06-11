import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { ManifestPageContent } from './ManifestPageContent';

export function ManifestPage() {
  return (
    <AppLayout>
      <ManifestPageContent />
    </AppLayout>
  );
}
