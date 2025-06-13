import { useManifest } from '@appV2/components/Pages/Manifests/useManifest';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { typeToDescription } from '@appV2/utils/Utils';
import {
  ActionList,
  ActionListItem,
  Alert,
  Button,
  ClipboardCopyButton,
  CodeBlock,
  CodeBlockAction,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  ExpandableSection,
  Grid,
  GridItem,
  HelperText,
  HelperTextItem,
  Label,
  PageSection,
  Panel,
  PanelMain,
  PanelMainBody,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import { CopyIcon, DownloadIcon, ExternalLinkSquareAltIcon, InfoIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';


const ManifestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request: manifest, error, loading }] = useManifest(id!);

  useDocumentTitle('SBOMer | Manifests | ' + id);




  if (error) {
    return (
      <Alert isExpandable variant="warning" title="Cannot retrieve manifest">
        <p>{error.message}.</p>
      </Alert>
    );
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading..." />;
  }

  if (!manifest) {
    return null;
  }

  return (
    <PageSection hasBodyWrapper={false}>
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Manifest {id}
          </Title>
        </GridItem>
        <GridItem span={12}>
          <DescriptionList
            columnModifier={{
              default: '2Col',
            }}
          >
            <DescriptionListGroup>
              <DescriptionListTerm>ID</DescriptionListTerm>
              <DescriptionListDescription>
                <pre>{manifest.id}</pre>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Created</DescriptionListTerm>
              <DescriptionListDescription>
                <Timestamp date={manifest.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                </Timestamp>
              </DescriptionListDescription>
            </DescriptionListGroup>

          </DescriptionList>

        </GridItem>
        <GridItem span={12}>
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Attributes</DescriptionListTerm>
              <DescriptionListDescription>
                <CodeBlock >
                  <CodeBlockCode>
                    {JSON.stringify(manifest, null, 2)}
                  </CodeBlockCode>
                </CodeBlock>
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { ManifestPageContent };
