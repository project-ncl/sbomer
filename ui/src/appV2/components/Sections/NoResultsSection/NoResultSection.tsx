import { Heading, Tile } from '@carbon/react';
import React from 'react';

export const NoResultsSection = () => {
  return (
    <Tile>
      <Heading>
        No Results Found
      </Heading>
      <p>
        No results found for the applied filters.
      </p>
    </Tile>
  );
};
