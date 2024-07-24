import { DefaultSbomerApi } from '@app/api/DefaultSbomerApi';
import { useGenerationRequestsLogPaths } from '@app/components/GenerationRequestTable/useGenerationRequestsLogPaths';
import { SbomerGenerationRequest } from '@app/types';
import { resultToDescription, statusToColor, statusToDescription, typeToDescription } from '@app/utils/Utils';
import { LazyLog } from '@melloware/react-logviewer';
import {
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
  Text,
  TextVariants,
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

const Logs = ({ request }: { request: SbomerGenerationRequest }) => {
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
              loadingComponent={<Skeleton screenreaderText={`Loading ${logPath} log...`} />}
              caseInsensitive
              enableHotKeys
              enableSearch
              extraLines={1}
              height="500"
              onLineContentClick={function noRefCheck() {}}
              selectableLines
              url={`${DefaultSbomerApi.getInstance().getBaseUrl()}/api/v1alpha3/sboms/requests/${request.id}/logs/${encodeURIComponent(logPath)}`}
            />
          </Tab>
        ))}
      </Tabs>
    </>
  );
};

const GeneratedManifests = ({ request }: { request: SbomerGenerationRequest }) => {
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

const GenerationRequestPage: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request, error, loading }] = useGenerationRequest(id);

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
    <PageSection>
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
                <Timestamp date={request.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                </Timestamp>
              </DescriptionListDescription>
            </DescriptionListGroup>
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
                  <Text component={TextVariants.small}>{resultToDescription(request)}</Text>
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
                <Logs request={request} />
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

export { GenerationRequestPage };
