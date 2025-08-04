import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import {
  Grid,
  Column,
  InlineNotification,
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
import * as React from 'react';
import { useParams } from 'react-router-dom';
import { useGenerationRequest } from './useGenerationRequest';

const GenerationRequestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGenerationRequest(id!);

  useDocumentTitle('SBOMer | Generations | ' + id);

  if (error) {
    return (
      <InlineNotification
        kind="warning"
        title="Cannot retrieve Generation Request"
        subtitle={error.message}
        hideCloseButton
      />
    );
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
            Generation Request {id}
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
                <StructuredListCell>ID</StructuredListCell>
                <StructuredListCell>
                  <code style={{ fontFamily: 'monospace' }}>{request.id}</code>
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>Created</StructuredListCell>
                <StructuredListCell>
                  {request.created ? request.created.toLocaleString() : 'N/A'}
                </StructuredListCell>
              </StructuredListRow>
            </StructuredListBody>
          </StructuredListWrapper>
        </Column>

        <Column sm={4} md={8} lg={16} style={{ marginTop: '2rem' }}>
          <Heading style={{ marginBottom: '1rem' }}>Attributes</Heading>
          <CodeSnippet type="multi">{JSON.stringify(request, null, 2)}</CodeSnippet>
        </Column>
      </Grid>
    </Content>
  );
};

export { GenerationRequestPageContent };
