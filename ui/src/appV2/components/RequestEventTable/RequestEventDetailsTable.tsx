import { useRequestEventManifest } from '@appV2/components/RequestEventTable/useRequestEventManifest';
import {
  Grid,
  Column,
  SkeletonText,
  CodeSnippet,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Content,
  Heading,
} from '@carbon/react';

import React from 'react';
import { useParams } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';

export const RequestEventDetailsTable = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, loading, error }] = useRequestEventManifest(id!);

  if (error) {
    return <ErrorSection error={error} />;
  }

  if (loading) {
    return <SkeletonText />;
  }

  if (!request) {
    return null;
  }

  return (
    <Content>
      <Grid>
        <Column sm={4} md={8} lg={16}>
          <Heading style={{ marginBottom: '2rem' }}>
            Event {id}
          </Heading>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <StructuredListWrapper>
            <StructuredListHead>
              <StructuredListRow head>
                <StructuredListCell head>Property</StructuredListCell>
                <StructuredListCell head>Value</StructuredListCell>
              </StructuredListRow>
            </StructuredListHead>
            <StructuredListBody>
              <StructuredListRow>
                <StructuredListCell>Event ID</StructuredListCell>
                <StructuredListCell>
                  <code style={{ fontFamily: 'monospace' }}>{id}</code>
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>Event Received At</StructuredListCell>
                <StructuredListCell>
                  {request.created ? request.created.toLocaleString() : 'N/A'}
                </StructuredListCell>
              </StructuredListRow>
            </StructuredListBody>
          </StructuredListWrapper>
        </Column>

        <Column sm={4} md={8} lg={16} style={{ marginTop: '2rem' }}>
          <Heading style={{ marginBottom: '1rem' }}>Attributes</Heading>
          <CodeSnippet type="multi">
            {JSON.stringify(request, null, 2)}
          </CodeSnippet>
        </Column>
      </Grid>
    </Content>
  );
};
