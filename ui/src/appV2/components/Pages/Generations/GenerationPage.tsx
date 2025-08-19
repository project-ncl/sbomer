import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { GenerationPageContent } from '@appV2/components/Pages/Generations/GenerationPageContent';
import * as React from 'react';


export function GenerationPage() {
  return (
    <AppLayout>
      <GenerationPageContent />
    </AppLayout>
  );
}
