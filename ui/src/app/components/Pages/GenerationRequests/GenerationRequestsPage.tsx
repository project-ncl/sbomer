import { GenerationRequestTable } from '@app/components/GenerationRequestTable/GenerationRequestTable';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';
import { Grid, GridItem, PageSection, Title } from '@patternfly/react-core';
import * as React from 'react';
import { AppLayout } from '../AppLayout/AppLayout';

export function GenerationRequestsPage() {
  useDocumentTitle('SBOMer | Generations');

  return (
    <AppLayout>
      <PageSection hasBodyWrapper={false}>
        <Grid hasGutter span={12}>
          <GridItem span={12}>
            <Title headingLevel="h1" size="4xl">
              Generations
            </Title>
          </GridItem>
          <GridItem span={12}>
            <GenerationRequestTable />
          </GridItem>
        </Grid>
      </PageSection>
    </AppLayout>
  );
}
