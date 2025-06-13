import { useRequestEventManifest } from '@appV2/components/RequestEventTable/useRequestEventManifest';
import {
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Timestamp,
  TimestampTooltipVariant,
  Title,
} from '@patternfly/react-core';

import React from 'react';
import { useParams } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';

export const RequestEventDetailsTable = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, loading, error }] = useRequestEventManifest(id!);

  if (error) {
    return <ErrorSection error={error} />;
  }

  return (
    <Grid hasGutter span={12}>
      <GridItem span={12}>
        <Title headingLevel="h1" size="4xl">
          Request Event {id}
        </Title>
      </GridItem>
      <GridItem span={12}>
        <DescriptionList columnModifier={{ default: '2Col' }}>
          <DescriptionListGroup>
            <DescriptionListTerm>Request Event ID</DescriptionListTerm>
            <DescriptionListDescription>
              <pre>{id}</pre>
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Request Event Received At</DescriptionListTerm>
            <DescriptionListDescription>
              <Timestamp date={request?.created} tooltip={{ variant: TimestampTooltipVariant.default }} />
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </GridItem>
      <GridItem>
        <DescriptionList >
          <DescriptionListGroup>
            <DescriptionListTerm>Attributes</DescriptionListTerm>
            <DescriptionListDescription>
              {request ? (
                <CodeBlock>
                  <CodeBlockCode>
                    {JSON.stringify(request, null, 2)}
                  </CodeBlockCode>
                </CodeBlock>
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
