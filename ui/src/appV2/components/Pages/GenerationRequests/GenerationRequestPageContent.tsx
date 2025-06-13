import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { resultToDescription, statusToColor, statusToDescription, typeToDescription } from '@appV2/utils/Utils';
import { LazyLog } from '@melloware/react-logviewer';
import {
  Alert,
  Button,
  ClipboardCopyButton,
  CodeBlock,
  CodeBlockAction,
  CodeBlockCode,
  Content,
  ContentVariants,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  ExpandableSection,
  Grid,
  GridItem,
  Label,
  List,
  ListItem,
  PageSection,
  Panel,
  PanelMain,
  PanelMainBody,
  Skeleton,
  Tab,
  TabTitleText,
  Tabs,
  Timestamp,
  TimestampTooltipVariant,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import { ExternalLinkSquareAltIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useGenerationRequest } from './useGenerationRequest';
import { useGenerationRequestManifests } from './useGenerationRequestManifests';
import { DefaultSbomerApiV2 } from 'src/appV2/api/DefaultSbomerApiV2';
import { useGenerationRequestsLogPaths } from '../../GenerationRequestTable/useGenerationRequestsLogPaths';
import { SbomerGeneration } from '@appV2/types';

const Logs = ({ request }: { request: SbomerGeneration }) => {
  const [{ logPaths, loading, error }] = useGenerationRequestsLogPaths(request);

  if (error) {
    return (
      <Alert isExpandable variant="warning" title="Unable to retrieve log">
        <p>{error.message}</p>
      </Alert>
    );
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading..." />;
  }

  if (!logPaths) {
    return null;
  }

  if (logPaths.length == 0) {
    return <Alert variant="info" title="No logs available" />;
  }

  return (
    <>
      <Tabs defaultActiveKey={0} mountOnEnter>
        {logPaths.map((logPath, index) => (
          <Tab key={index} eventKey={index} title={<TabTitleText>{logPath}</TabTitleText>}>
            <LazyLog
              caseInsensitive
              enableHotKeys
              enableSearch
              extraLines={1}
              height="500"
              onLineContentClick={function noRefCheck() {}}
              selectableLines
              url={`${DefaultSbomerApiV2.getInstance().getBaseUrl()}/api/v1beta2/generations/${request.id}/logs/${encodeURIComponent(logPath)}`}
            />
          </Tab>
        ))}
      </Tabs>
    </>
  );
};

const GeneratedManifests = ({ request }: { request: SbomerGeneration }) => {
  const [{ manifests, total, loading, error }] = useGenerationRequestManifests(request.id);

  if (error) {
    return (
      <Alert isExpandable variant="warning" title="Unable to retrieve list of generated manifests">
        <p>{error.message}</p>
      </Alert>
    );
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading..." />;
  }

  if (!manifests || total == 0) {
    return <Alert variant="info" title="No manifests available" />;
  }

  return (
    <>
      <List>
        {manifests.map((manifest) => (
          <ListItem>
            <Button
              variant="link"
              icon={<ExternalLinkSquareAltIcon />}
              iconPosition="end"
              component={(props: any) => <Link {...props} to={`/manifests/${manifest.id}`} />}
            >
              {manifest.id}
            </Button>
          </ListItem>
        ))}
      </List>
    </>
  );
};

const GenerationRequestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGenerationRequest(id!);

  useDocumentTitle('SBOMer | Generations | ' + id);

  const [copied, setCopied] = useState(false);

  const clipboardCopyFunc = (event, text) => {
    navigator.clipboard.writeText(text.toString());
  };

  const onClick = (event, text) => {
    clipboardCopyFunc(event, text);
    setCopied(true);
  };

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
          <DescriptionList
            columnModifier={{
              default: '2Col',
            }}
          >
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
            {request.updatedTime && (
              <DescriptionListGroup>
                <DescriptionListTerm>Updated</DescriptionListTerm>
                <DescriptionListDescription>
                  <Timestamp date={request.updatedTime} tooltip={{ variant: TimestampTooltipVariant.default }} />
                </DescriptionListDescription>
              </DescriptionListGroup>
            )}
            {request.finishedTime && (
              <DescriptionListGroup>
                <DescriptionListTerm>Finished</DescriptionListTerm>
                <DescriptionListDescription>
                  <Timestamp date={request.finishedTime} tooltip={{ variant: TimestampTooltipVariant.default }} />
                </DescriptionListDescription>
              </DescriptionListGroup>
            )}
            <DescriptionListGroup>
              <DescriptionListTerm>Type</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content={request.type}>
                  <Label style={{ cursor: 'pointer' }} color="purple">
                    {typeToDescription(request)}
                  </Label>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Identifier</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content="Main identifier used as a source for the manifest generation. It's related to the TYPE.">
                  <span className="pf-v5-c-timestamp pf-m-help-text">{request.identifier}</span>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Status</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content={request.status}>
                  <Label style={{ cursor: 'pointer' }} color={statusToColor(request)}>
                    {statusToDescription(request)}
                  </Label>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Result</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip content={request.result} hidden={request.result == null}>
                  <Content component={ContentVariants.small}>{resultToDescription(request)}</Content>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Reason</DescriptionListTerm>
              <DescriptionListDescription>{request.reason}</DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>

          <br />

          <Panel>
            <PanelMain>
              <PanelMainBody>
                <Title headingLevel="h2">Generated Manifests</Title>
                <br />
                <GeneratedManifests request={request} />
              </PanelMainBody>
            </PanelMain>
          </Panel>

          <br />

          <Panel>
            <PanelMain>
              <PanelMainBody>
                <Title headingLevel="h2">Logs</Title>
                <br />
                {request.status == "GENERATING" ? <Alert variant="warning" title='Logs are unavailable while generation has status: "In progress"'>
                </Alert> : <Logs request={request} />}
              </PanelMainBody>
            </PanelMain>
          </Panel>

          <br />

          <Panel>
            <PanelMain>
              <PanelMainBody>
                <Title headingLevel="h2">Raw content</Title>
                <br />
                <CodeBlock
                  actions={
                    <>
                      <CodeBlockAction>
                        <ClipboardCopyButton
                          id="copy-button"
                          textId="code-content"
                          aria-label="Copy to clipboard"
                          onClick={(e) => onClick(e, JSON.stringify(request, null, 2))}
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
                    <CodeBlockCode>{JSON.stringify(request, null, 2)}</CodeBlockCode>
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

export { GenerationRequestPageContent };
