import { Card, CardBody, CardTitle } from '@patternfly/react-core';
import React from 'react';

export const NoResultsSection = () => {
  return (
    <Card>
      <CardTitle>No Results Found</CardTitle>
      <CardBody>
        No results found for the applied filters.
      </CardBody>
    </Card>
  );
};
