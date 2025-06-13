import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import {
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Title,
  Alert,
  PageSection,
} from '@patternfly/react-core';
import * as React from 'react';
import { useParams } from 'react-router-dom';
import { useGenerationRequest } from './useGenerationRequest';

const GenerationRequestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGenerationRequest(id!);

  useDocumentTitle('SBOMer | Generations | ' + id);

  if (error) {
    return (
      <Alert isExpandable variant="warning" title="Cannot retrieve Generation Request">
        <p>{error.message}.</p>
      </Alert>
    );
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading..." />;
  }

  if (!request) {
    return null;
  }

  return (
    <PageSection hasBodyWrapper={false}>
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Generation Request {id}
          </Title>
        </GridItem>
        <GridItem span={12}>
          <DescriptionList columnModifier={{ default: '2Col' }}>
            <DescriptionListGroup>
              <DescriptionListTerm>ID</DescriptionListTerm>
              <DescriptionListDescription>
                <pre>{request.id}</pre>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Created</DescriptionListTerm>
              <DescriptionListDescription>
                <Timestamp date={request.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }} />
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </GridItem>
        <GridItem span={12}>
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Attributes</DescriptionListTerm>
              <DescriptionListDescription>
                <CodeBlock>
                  <CodeBlockCode>{JSON.stringify(request, null, 2)}</CodeBlockCode>
                </CodeBlock>
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { GenerationRequestPageContent };
