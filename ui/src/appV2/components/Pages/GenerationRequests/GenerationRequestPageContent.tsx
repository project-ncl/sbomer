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
    <Grid hasGutter span={12}>
      <GridItem span={12}>
        <Title headingLevel="h1" size="4xl">
          Generation Request {id}
        </Title>
      </GridItem>
      <GridItem span={12}>
        <DescriptionList columnModifier={{ default: '2Col' }}>
          <DescriptionListGroup>
            <DescriptionListTerm>Generation Request ID</DescriptionListTerm>
            <DescriptionListDescription>
              <pre>{id}</pre>
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Generation Request Created At</DescriptionListTerm>
            <DescriptionListDescription>
              <Timestamp date={request?.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Attributes</DescriptionListTerm>
            <DescriptionListDescription>
              {request ? (
                <DescriptionList isHorizontal>
                  {Object.entries(request).map(([key, value]) => (
                    <DescriptionListGroup key={key}>
                      <DescriptionListTerm>{key}</DescriptionListTerm>
                      <DescriptionListDescription>
                        <pre style={{ margin: 0 }}>{JSON.stringify(value, null, 2)}</pre>
                      </DescriptionListDescription>
                    </DescriptionListGroup>
                  ))}
                </DescriptionList>
              ) : (
                <span>No request attributes available</span>
              )}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </GridItem>
    </Grid>
  );
};

export { GenerationRequestPageContent };
