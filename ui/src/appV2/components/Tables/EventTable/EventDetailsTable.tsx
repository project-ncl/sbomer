import { useRequestEventManifest } from '@appV2/components/Tables/EventTable/useEventManifest';
import {
  CodeSnippet,
  Heading,
  SkeletonText,
  Stack,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
} from '@carbon/react';

import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import RelativeTimestamp from '@appV2/components/UtilsComponents/RelativeTimestamp';
import { eventStatusToColor } from '@appV2/utils/Utils';
import React from 'react';
import { useParams } from 'react-router-dom';

export const EventDetailsTable = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, loading, error }] = useRequestEventManifest(id!);

  if (error) {
    return <ErrorSection title="Could not load event details" message={error.message} />;
  }

  if (loading) {
    return <SkeletonText />;
  }

  if (!request) {
    return null;
  }

  return (
    <Stack gap={7}>
      <Heading>Event {id}</Heading>
        <StructuredListWrapper>
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>Property</StructuredListCell>
                  <StructuredListCell head>Value</StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>ID</StructuredListCell>
                  <StructuredListCell>
                    <span>
                      {id}
                    </span>
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Created</StructuredListCell>
                  <StructuredListCell>
                    {request.created ? (
                      <Stack gap={2}>
                        <RelativeTimestamp date={request.created} />
                        <span>{request.created.toISOString()}</span>
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
                        <span>{request.updated.toISOString()}</span>
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
        <Stack gap={5}>
          <Heading>Attributes</Heading>
          <CodeSnippet type="multi">
            {JSON.stringify(request, null, 2)}
          </CodeSnippet>
        </Stack>
      </Stack>
  );
};
