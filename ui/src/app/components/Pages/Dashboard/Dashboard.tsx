import { AboutSection } from '@app/components/Sections/AboutSection/AboutSection';
import { GenerationRequestTable } from '@app/components/GenerationRequestTable/GenerationRequestTable';
import { StatsSection } from '@app/components/Sections/StatsSection/StatsSection';
import { PageSection, Title } from '@patternfly/react-core';
import { Grid, GridItem } from '@patternfly/react-core';
import * as React from 'react';

const Dashboard: React.FunctionComponent = () => (
  <PageSection>
    <Grid hasGutter span={12}>
      <GridItem span={12}>
        <Title headingLevel="h1" size="4xl">
          SBOMer
        </Title>
      </GridItem>
      <GridItem span={12}>
        <AboutSection />
      </GridItem>
      <GridItem span={12}>
        <StatsSection />
      </GridItem>
      <GridItem span={12}>
        <GenerationRequestTable />
      </GridItem>
    </Grid>
  </PageSection>
);

export { Dashboard };
