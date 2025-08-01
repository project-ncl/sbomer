import { Tile, Grid, Column } from '@carbon/react';
import React from 'react';

export const NoResultsSection = () => {
  return (
    <Tile>
      <Grid>
        <Column sm={4} md={8} lg={16}>
          <h3 style={{ marginBottom: '1rem' }}>No Results Found</h3>
          <p>No results found for the applied filters.</p>
        </Column>
      </Grid>
    </Tile>
  );
};
