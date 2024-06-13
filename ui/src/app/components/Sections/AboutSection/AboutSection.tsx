import { Card, CardBody, CardTitle, Grid, GridItem } from '@patternfly/react-core';
import React from 'react';

export const AboutSection = () => {
  return (
    <Card isLarge>
      <CardTitle size={20}>About</CardTitle>
      <CardBody>
        <Grid cellSpacing={3} span={12}>
          <GridItem span={12}>
            A service to generate <strong>manifests</strong> in the <strong>CycloneDX</strong> format for Application
            Services products.
          </GridItem>
        </Grid>
      </CardBody>
    </Card>
  );
};
