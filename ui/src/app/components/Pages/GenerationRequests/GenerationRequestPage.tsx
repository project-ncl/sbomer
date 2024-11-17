import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { GenerationRequestPageContent } from './GenerationRequestPageContent';

export function GenerationRequestPage() {
  return (
    <AppLayout>
      <GenerationRequestPageContent />
    </AppLayout>
  );
}
