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
  Tag,
} from '@carbon/react';

import React from 'react';
import { useParams } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { eventStatusToColor } from '@appV2/utils/Utils';
import RelativeTimestamp from '@appV2/components/UtilsComponents/RelativeTimestamp';

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
                  <StructuredListCell>Created</StructuredListCell>
                  <StructuredListCell>
                    {request.created ? (
                      <Stack gap={2}>
                        <RelativeTimestamp date={request.created} />
                        <p>{request.created.toISOString()}</p>
                      </Stack>
                    ) : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Updated</StructuredListCell>
                  <StructuredListCell>
                    {request.updated ? (
                      <Stack gap={2}>
                        <RelativeTimestamp date={request.updated} />
                        <p>{request.updated.toISOString()}</p>
                      </Stack>
                    ) : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Finished</StructuredListCell>
                  <StructuredListCell>
                    {request.finished ? (
                      <Stack gap={2}>
                        <RelativeTimestamp date={request.finished} />
                        <p>{request.finished.toISOString()}</p>
                      </Stack>
                    ) : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Status</StructuredListCell>
                  <StructuredListCell>
                    <Tag size='md' type={eventStatusToColor(request.status)}>
                      {request.status}
                    </Tag>
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
