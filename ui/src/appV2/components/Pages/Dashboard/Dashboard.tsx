import { AboutSection } from '@appV2/components/Sections/AboutSection/AboutSection';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';
import {
  Column,
  Grid,
  InlineNotification,
  Content,
  Heading,
  Stack,
} from '@carbon/react';

const Dashboard: React.FunctionComponent = () => {
  useDocumentTitle("SBOMER | Dashboard");

  return (
      <AppLayout>
        <Content>
          <Stack gap={7}>
            <Grid>
              <Column sm={4} md={8} lg={16}>
                <Stack gap={4}>
                  <Heading>
                    SBOMer Next Generation
                  </Heading>
                </Stack>
              </Column>
            </Grid>

            <Grid>
              <Column sm={4} md={8} lg={16}>
                <InlineNotification
                  kind="info"
                  title="In Development"
                  subtitle="Features are actively being developed and may change."
                  hideCloseButton
                />
              </Column>
            </Grid>

            <Grid>
              <Column sm={4} md={8} lg={6}>
                <AboutSection />
              </Column>
            </Grid>


          </Stack>
        </Content>
      </AppLayout>
  );
};

export { Dashboard };
