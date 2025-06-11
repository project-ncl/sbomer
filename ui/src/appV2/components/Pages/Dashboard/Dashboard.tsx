import { AboutSection } from '@app/components/Sections/AboutSection/AboutSection';
import { StatsSection } from '@app/components/Sections/StatsSection/StatsSection';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';

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
          <GridItem span={6}>
            <StatsSection />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
};

export { Dashboard };
