import React from 'react';
import { Grid, Column, CodeSnippet, Button } from '@carbon/react';
import { Link as RouterLink } from 'react-router-dom';

interface QueryExampleProps {
  description: string;
  query: string;
}

export const QueryExample: React.FC<QueryExampleProps> = ({ description, query }) => {
  const encodedQuery = encodeURIComponent(query);
  const to = `/events?query=${encodedQuery}`;

  return (
    <>
      <p>{description}</p>
      <Grid narrow>
        <Column sm={2} md={6} lg={12}>
          <CodeSnippet type="single">{query}</CodeSnippet>
        </Column>
        <Column sm={2} md={2} lg={4}>
          <Button as={RouterLink} to={to} size="md" kind="primary">
            Run Query
          </Button>
        </Column>
      </Grid>
    </>
  );
};
