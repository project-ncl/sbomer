import { Card, CardBody, CardTitle, Grid, GridItem } from '@patternfly/react-core';
import React from 'react';

export const ErrorSection = () => {
  return (
    <Card>
      <CardTitle size={10}>An error occurred</CardTitle>
      <CardBody>
        <Grid cellSpacing={3} span={12}>
          <GridItem span={12}>
            TBD
          </GridItem>
        </Grid>
      </CardBody>
    </Card>
  );
};
