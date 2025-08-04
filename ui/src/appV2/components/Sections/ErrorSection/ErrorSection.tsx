import { Card, CardBody, CardTitle, Grid, GridItem } from '@patternfly/react-core';
import React from 'react';

export const ErrorSection = ({ error }: { error: Error }) => {
  return (
    <Card>
      <CardTitle size={10}>An error occurred: {error.name}</CardTitle>
      <CardBody>
        <Grid cellSpacing={3} span={12}>
          <GridItem span={12}>
            {error.message}
          </GridItem>
        </Grid>
      </CardBody>
    </Card>
  );
};
