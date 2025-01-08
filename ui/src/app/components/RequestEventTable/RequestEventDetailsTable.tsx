import { requestEventStatusToColor, requestEventStatusToDescription, statusToColor, statusToDescription, typeToDescription, timestampToHumanReadable } from '@app/utils/Utils';
import {
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  ExpandableSection,
  Grid,
  GridItem,
  Label,
  Skeleton,
  Tab,
  TabTitleText,
  Tabs,
  Timestamp,
  TimestampTooltipVariant,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useRequestEventManifest } from './useRequestEventManifest';
import { useRequestEventGeneration } from './useRequestEventGeneration';


const columnNames = {
  id: 'Generation ID',
  type: 'Type',
  identifier: 'Generation Identifier',
  status: 'Generation Status',
  creationTime: 'Generation Created at',
  mId: 'Manifest ID',
  mRootPurl: 'Manifest Purl',
  mIdentifier: 'Manifest Identifier',
  mCreationTime: 'Manifest Created at'
};

export const RequestEventDetailsTable = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request: sbomerRequestManifest, loading, error }] = useRequestEventManifest(id!);
  const [{ value: sbomerRequestGenerations, loading: loadingGeneration, error: errorGeneration }] = useRequestEventGeneration(id!);

  if (error || errorGeneration) {
    return <ErrorSection />;
  }

  if (loading || loadingGeneration) {
    return <Skeleton screenreaderText="Loading data..." />;
  }

  if (!sbomerRequestManifest || !sbomerRequestGenerations) {
    return null;
  }

  // UseState does not seem to work, keep expansion toggling state
  const expandedGenerations = {};
  const toggleExpandable = (generationId: string) => {
    expandedGenerations[generationId] = !expandedGenerations[generationId];
  };

  return (
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Request Event {id}
          </Title>
        </GridItem>
        <GridItem span={12}>
          <DescriptionList
            columnModifier={{
              default: '2Col',
            }}
          >
            <DescriptionListGroup>
              <DescriptionListTerm>Request Event ID</DescriptionListTerm>
              <DescriptionListDescription>
                <pre>{id}</pre>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Request Event Received At</DescriptionListTerm>
              <DescriptionListDescription>
                <Timestamp date={sbomerRequestManifest.reqReceivalTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                </Timestamp>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Request Event Source</DescriptionListTerm>
              <DescriptionListDescription>
                <Label style={{ cursor: 'pointer' }} color="yellow">
                  {sbomerRequestManifest.reqEventType}
                </Label>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Request Event Config</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip isContentLeftAligned={true} content={<code>{sbomerRequestManifest.reqConfig}</code>}>
                  <Label>
                    {sbomerRequestManifest.reqConfigTypeName}={sbomerRequestManifest.reqConfigTypeValue}
                  </Label>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>

            <DescriptionListGroup>
              <DescriptionListTerm>Request Event Status</DescriptionListTerm>
              <DescriptionListDescription>
              <Tooltip
                  isContentLeftAligned={true}
                  content={
                    <div>
                      <div>
                        <strong>{sbomerRequestManifest.reqEventStatus}</strong>
                      </div>
                    </div>
                  }
                >
                  <Label style={{ cursor: 'pointer' }} color={requestEventStatusToColor(sbomerRequestManifest.reqEventStatus)}>
                    {requestEventStatusToDescription(sbomerRequestManifest.reqEventStatus)}
                  </Label>
                  {/* <span className="pf-v5-c-timestamp pf-m-help-text">{requestEvent.eventStatus}</span> */}
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Request Event Status Reason</DescriptionListTerm>
              <DescriptionListDescription>
              {sbomerRequestManifest.reqReason}
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Event</DescriptionListTerm>
              <DescriptionListDescription>
              <CodeBlock>
                  <ExpandableSection toggleTextExpanded="Hide" toggleTextCollapsed="Show">
                    <CodeBlockCode>{JSON.stringify(sbomerRequestManifest.reqEvent, null, 2).replace(/\\"/g, '"')}</CodeBlockCode>
                  </ExpandableSection> 
                </CodeBlock>
              </DescriptionListDescription>
            </DescriptionListGroup>
            </DescriptionList>
          <br /><br /><br />

          <Tabs defaultActiveKey={0} mountOnEnter>
       
            <Tab key={0} eventKey={0} title={<TabTitleText>Generations triggered</TabTitleText>}>
              <Table aria-label="Generations table" variant="compact">
                <Caption></Caption>
                <Thead>
                  <Tr>
                    <Th>{columnNames.id}</Th>
                    <Th>{columnNames.type}</Th>
                    <Th>{columnNames.identifier}</Th>
                    <Th>{columnNames.status}</Th>
                    <Th>{columnNames.creationTime}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                {sbomerRequestGenerations.data.map((generation) => (
                    <Tr key={generation.id}>
                      <Td dataLabel={columnNames.id}>
                        <Link 
                          to={`/generations/${generation.id}`} 
                          style={{ textDecoration: 'none', color: 'blue' }}
                        >
                        <pre>{generation.id}</pre>
                        </Link>
                      </Td>
                      <Td dataLabel={columnNames.type}>
                        <Tooltip content={generation.type}>
                          <Label style={{ cursor: 'pointer' }} color="purple">
                            {typeToDescription(generation)}
                          </Label>
                        </Tooltip>
                      </Td>
                      <Td dataLabel={columnNames.identifier}>
                        <span className="pf-v5-c-timestamp pf-m-help-text">{generation.identifier}</span>
                      </Td>
                      <Td dataLabel={columnNames.status}>
                        <Tooltip
                          isContentLeftAligned={true}
                          content={
                            <div>
                              <div>
                                <strong>{generation.result}</strong>
                              </div>
                              <div>{generation.reason}</div>
                            </div>
                          }
                        >
                          <Label style={{ cursor: 'pointer' }} color={statusToColor(generation)}>
                            {statusToDescription(generation)}
                          </Label>
                        </Tooltip>
                      </Td>
                      <Td dataLabel={columnNames.creationTime}>
                        <Timestamp date={generation.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                          {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                        </Timestamp>
                      </Td>
                      {/* Expandable Section for Children (e.g., linked manifests) */}
                      <Td colSpan={5}>
                        <ExpandableSection
                          toggleText={expandedGenerations[generation.id] ? 'Hide Manifests' : 'Show Manifests'}
                          onToggle={() => {toggleExpandable(generation.id);}}
                        >
                        {sbomerRequestManifest.manifests.length > 0 ? (
                        <div>
                          <Table aria-label="Child Manifests" variant="compact">
                            <Thead>
                              <Tr>
                                <Th>{columnNames.mId}</Th>
                                <Th>{columnNames.mRootPurl}</Th>
                              </Tr>
                            </Thead>
                            <Tbody>
                              {sbomerRequestManifest.manifests
                                .filter((manifest) => manifest.generation.id === generation.id)
                                .map((manifest) => (
                                  <Tr key={manifest.id}>
                                    <Td dataLabel={columnNames.mId}>
                                      <Link to={`/manifests/${manifest.id}`} style={{ textDecoration: 'none', color: 'blue' }}>
                                        <pre>{manifest.id}</pre>
                                      </Link>
                                    </Td>
                                    <Td dataLabel={columnNames.mRootPurl}>
                                      <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.rootPurl}</span>
                                    </Td>
                                  </Tr>
                                ))}
                            </Tbody>
                          </Table>
                        </div>
                      ) : (
                        <p>No child manifests available</p>
                      )}
                        </ExpandableSection>
                      </Td>
                    </Tr>
                  ))} 
                </Tbody>
              </Table>
            </Tab>
            <Tab key={1} eventKey={1} title={<TabTitleText>All Manifests generated</TabTitleText>}>
              <Table aria-label="Manifests table" variant="compact">
                <Caption></Caption>
                <Thead>
                  <Tr>
                    <Th>{columnNames.mId}</Th>
                    <Th>{columnNames.mRootPurl}</Th>
                    <Th>{columnNames.mCreationTime}</Th>
                    <Th>{columnNames.id}</Th>
                    <Th>{columnNames.type}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {sbomerRequestManifest.manifests.map((manifest) => (
                    <Tr key={manifest.id}>
                      <Td dataLabel={columnNames.mId}>
                        <Link 
                          to={`/manifests/${manifest.id}`} 
                          style={{ textDecoration: 'none', color: 'blue' }}
                        >
                        <pre>{manifest.id}</pre>
                        </Link>
                      </Td>
                      <Td dataLabel={columnNames.mRootPurl}>
                        <Tooltip
                          isContentLeftAligned={true}
                          content={
                            <div>
                              <div>
                                <strong>Identifier</strong>
                              </div>
                              <div>{manifest.identifier}</div>
                            </div>
                          }
                        >
                          <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.rootPurl}</span>
                        </Tooltip>
                      </Td>
                      <Td dataLabel={columnNames.mCreationTime}>
                        <Timestamp date={manifest.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                          {/*timestampToHumanReadable(Date.now() - new Date(manifest.creationTime).getTime(), false, 'ago')*/}
                        </Timestamp>
                      </Td>
                      <Td dataLabel={columnNames.id}>
                        <Link 
                            to={`/generations/${manifest.generation.id}`} 
                            style={{ textDecoration: 'none', color: 'blue' }}
                          >
                          <pre>{manifest.generation.id}</pre>
                          </Link>
                      </Td>
                      <Td dataLabel={columnNames.type}>
                        <Tooltip content={manifest.generation.type}>
                          <Label style={{ cursor: 'pointer' }} color="purple">
                            {typeToDescription(manifest.generation)}
                          </Label>
                        </Tooltip>
                      </Td>

                    </Tr>
                  ))} 
                </Tbody>
              </Table>
            </Tab>
       
          </Tabs>

      
      </GridItem>
      </Grid>
    
  );
};
