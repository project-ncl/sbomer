import { Tile, Grid, Column } from '@carbon/react';
import React from 'react';

export const ErrorSection = ({ error }: { error: Error }) => {
  return (
    <Tile>
      <Grid>
        <Column sm={4} md={8} lg={16}>
          <h3 style={{ marginBottom: '1rem' }}>An error occurred: {error.name}</h3>
          <p>{error.message}</p>
        </Column>
      </Grid>
    </Tile>
  );
};
