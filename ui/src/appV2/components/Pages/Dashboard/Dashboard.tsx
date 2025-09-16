import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { AboutSection } from '@appV2/components/Sections/AboutSection/AboutSection';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import {
  Heading,
  InlineNotification,
  Stack
} from '@carbon/react';
import * as React from 'react';

const Dashboard: React.FunctionComponent = () => {
  useDocumentTitle("SBOMer | Dashboard");

  return (
    <AppLayout>
      <Stack gap={7}>
        <Heading>SBOMer</Heading>
        <InlineNotification
          kind="info"
          title="In Development"
          subtitle="Features are actively being developed and may change."
          hideCloseButton
        />
        <AboutSection />
      </Stack>
    </AppLayout>
  );
};

export { Dashboard };
