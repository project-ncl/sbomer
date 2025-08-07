import { useRequestEventManifest } from '@appV2/components/EventTable/useEventManifest';
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
  Stack,
} from '@carbon/react';

import React from 'react';
import { useParams } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';

export const EventDetailsTable = () => {
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
      <Stack gap={7}>
        <Grid>
          <Column sm={4} md={8} lg={16}>
            <Heading>Event {id}</Heading>
          </Column>
        </Grid>

        <Grid>
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
                    <CodeSnippet type="inline" hideCopyButton>
                      {id}
                    </CodeSnippet>
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Event Received At</StructuredListCell>
                  <StructuredListCell>
                    {request.created ? request.created.toISOString() : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </Column>
        </Grid>

        <Grid>
          <Column sm={4} md={8} lg={16}>
            <Stack gap={5}>
              <Heading>Attributes</Heading>
              <CodeSnippet type="multi">
                {JSON.stringify(request, null, 2)}
              </CodeSnippet>
            </Stack>
          </Column>
        </Grid>
      </Stack>
    </Content>
  );
};
