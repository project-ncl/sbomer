import { AboutSection } from '@appV2/components/Sections/AboutSection/AboutSection';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import { Column, Grid, InlineNotification, Tile, ToastNotification } from '@carbon/react';

const Dashboard: React.FunctionComponent = () => {

  useDocumentTitle("SBOMER | Dashboard")

  return (
    <AppLayout>
        <Grid>
          <Column span={12}>
            <h1>SBOMer Next Generation</h1>
          </Column>
          <Column span={8}>
            <Tile>
              IN DEVELOPMENT
            </Tile>
          </Column>
          <Column span={6}>
            <AboutSection />
          </Column>
        </Grid>
    </AppLayout>
  );
};

export { Dashboard };
