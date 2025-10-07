import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import RelativeTimestamp from '@appV2/components/UtilsComponents/RelativeTimestamp';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { resultToColor, statusToColor } from '@appV2/utils/Utils';
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
import * as React from 'react';
import { useParams } from 'react-router-dom';
import { useGeneration } from './useGeneration';
import { MetadataOverview } from '@appV2/components/UtilsComponents/MetadataOverview';

const GenerationPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGeneration(id!);

  useDocumentTitle('SBOMer | Generations | ' + id);

  if (error) {
    return <ErrorSection title="Could not load generations" message={error.message}  />;
  }

  if (loading) {
    return <SkeletonText />;
  }

  if (!request) {
    return null;
  }

  return (
    <Stack gap={7}>
      <Heading>Generation {id}</Heading>
      <StructuredListWrapper isCondensed>
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
                {request.id}
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
                  <span>{request.finished.toISOString()}</span>
                </Stack>
              ) : 'N/A'}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>Status</StructuredListCell>
            <StructuredListCell>
              <Tag size='md' type={statusToColor(request)}>
                {request.status}
              </Tag>
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>Result</StructuredListCell>
            <StructuredListCell>
              <Tag size='md' type={resultToColor(request)}>
                {request.result || 'In progress'}
              </Tag>
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListBody>
      </StructuredListWrapper>
      <MetadataOverview metadata={request.metadata} redirectPrefix='generations'/>
        <Stack gap={5}>
          <Heading>Raw JSON</Heading>
          <CodeSnippet type="multi">
            {JSON.stringify(
            request,
            (key, value) => {
              if (value instanceof Map) {
                return Object.fromEntries(value.entries());
              }
              return value;
            },
            2
          )}
          </CodeSnippet>
        </Stack>
    </Stack>
  );
};

export { GenerationPageContent as GenerationPageContent };
