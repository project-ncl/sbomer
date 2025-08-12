import { Tile } from '@carbon/react';
import React from 'react';

export const NoResultsSection = () => {
  return (
    <Tile>
      <h3>
        No Results Found
      </h3>
      <p>
        No results found for the applied filters.
      </p>
    </Tile>
  );
};
