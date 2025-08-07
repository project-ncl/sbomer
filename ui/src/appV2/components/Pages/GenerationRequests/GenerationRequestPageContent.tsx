import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
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
import * as React from 'react';
import { useParams } from 'react-router-dom';
import { useGenerationRequest } from './useGenerationRequest';
import { statusToColor, resultToColor } from '@appV2/utils/Utils';

const GenerationRequestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGenerationRequest(id!);

  useDocumentTitle('SBOMer | Generations | ' + id);

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
            <Heading>Generation Request {id}</Heading>
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
                  <StructuredListCell>ID</StructuredListCell>
                  <StructuredListCell>
                    <CodeSnippet type="inline" hideCopyButton>
                      {request.id}
                    </CodeSnippet>
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Created</StructuredListCell>
                  <StructuredListCell>
                    {request.created ? request.created.toISOString() : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Updated</StructuredListCell>
                  <StructuredListCell>
                    {request.updated ? request.updated.toISOString() : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Finished</StructuredListCell>
                  <StructuredListCell>
                    {request.finished ? request.finished.toISOString() : 'N/A'}
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

export { GenerationRequestPageContent };
