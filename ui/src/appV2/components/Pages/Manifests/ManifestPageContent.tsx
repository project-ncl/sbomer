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

  const [copied, setCopied] = useState(false);

  const clipboardCopyFunc = (event, text) => {
    navigator.clipboard.writeText(text.toString());
  };

  const onClick = (event, text) => {
    clipboardCopyFunc(event, text);
    setCopied(true);
  };

  const downloadManifest = (manifest) => {
    const element = document.createElement('a');
    const file = new Blob([JSON.stringify(manifest.sbom, null, 2)], { type: 'application/json' });
    element.href = URL.createObjectURL(file);
    element.download = manifest.id + '.json';
    document.body.appendChild(element);
    element.click();
  };

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

          <br />

          <Panel>
            <PanelMain>
              <PanelMainBody>

                <br />

                <Title headingLevel="h2">Raw content</Title>

                <br />

                <HelperText>
                  <HelperTextItem icon={<InfoIcon />}>
                    Please note that the raw content below contains the RAW body of the API response, which encapsulates
                    the manifest itself. If you are interested in the <strong>manifest content</strong>, please use the
                    download and copy buttons available above.
                  </HelperTextItem>
                </HelperText>

                <br />

                <CodeBlock

                >
                  <ExpandableSection toggleTextExpanded="Hide" toggleTextCollapsed="Show">
                    <CodeBlockCode>{JSON.stringify(manifest, null, 2)}</CodeBlockCode>
                  </ExpandableSection>
                </CodeBlock>
              </PanelMainBody>
            </PanelMain>
          </Panel>
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { ManifestPageContent };
