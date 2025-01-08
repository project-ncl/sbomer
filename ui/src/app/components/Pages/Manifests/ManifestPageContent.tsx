import { useDocumentTitle } from '@app/utils/useDocumentTitle';
import { typeToDescription } from '@app/utils/Utils';
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
import { useManifest } from './useManifest';

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
            <DescriptionListGroup>
              <DescriptionListTerm>Type</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content={manifest.generation.type}>
                  <Label style={{ cursor: 'pointer' }} color="purple">
                    {typeToDescription(manifest.generation)}
                  </Label>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Identifier</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content="Main identifier used as a source for the manifest generation. It's related to the TYPE.">
                  <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.identifier}</span>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Generation Request</DescriptionListTerm>
              <DescriptionListDescription>
                <Button
                  variant="link"
                  icon={<ExternalLinkSquareAltIcon />}
                  iconPosition="end"
                  component={(props: any) => <Link {...props} to={`/generations/${manifest.generation.id}`} />}
                >
                  {manifest.generation.id}
                </Button>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Purl</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content="Root purl">
                  <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.rootPurl}</span>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>

          <br />

          <Panel>
            <PanelMain>
              <PanelMainBody>
                <Title headingLevel="h2">Actions</Title>

                <br />

                <ActionList>
                  <ActionListItem>
                    <Button variant="primary" icon={<DownloadIcon />} onClick={(e) => downloadManifest(manifest)}>
                      {' '}
                      Download
                    </Button>
                  </ActionListItem>
                  <ActionListItem>
                    <Tooltip content="Click to copy manifest content">
                      <Button
                        variant="secondary"
                        icon={<CopyIcon />}
                        onClick={(e) => onClick(e, JSON.stringify(manifest.sbom, null, 2))}
                      >
                        {' '}
                        Copy
                      </Button>
                    </Tooltip>
                  </ActionListItem>
                </ActionList>

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
                  actions={
                    <>
                      <CodeBlockAction>
                        <ClipboardCopyButton
                          id="copy-button"
                          textId="code-content"
                          aria-label="Copy to clipboard"
                          onClick={(e) => onClick(e, JSON.stringify(manifest.sbom, null, 2))}
                          exitDelay={copied ? 1500 : 600}
                          maxWidth="110px"
                          variant="plain"
                          onTooltipHidden={() => setCopied(false)}
                        >
                          {copied ? 'Successfully copied to clipboard!' : 'Copy to clipboard'}
                        </ClipboardCopyButton>
                      </CodeBlockAction>
                    </>
                  }
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
