import { AboutSection } from '@appV2/components/Sections/AboutSection/AboutSection';
import { StatsSection } from '@appV2/components/Sections/StatsSection/StatsSection';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { AppLayout } from '@appV2/components/Pages/AppLayout/AppLayout';

const Dashboard: React.FunctionComponent = () => {

  useDocumentTitle("SBOMER | Dashboard")

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <Title headingLevel="h1" size="4xl">
              SBOMer
            </Title>
          </GridItem>
          <GridItem span={6}>
            <AboutSection />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
};

export { Dashboard };
