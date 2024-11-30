import { requestEventStatusToColor, requestEventStatusToDescription, statusToColor, statusToDescription, timestampToHumanReadable } from '@app/utils/Utils';
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


const columnNames = {
  mId: 'Manifest ID',
  mRootPurl: 'Purl',
  mType: 'Manifest Type',
  mIdentifier: 'Manifest Identifier',
  mCreationTime: 'Manifest Created at',
  gId: 'Generation ID',
  gType: 'Generation Type',
  gStatus: 'Generation Status',
  gCreationTime: 'Generation Created at',
};

export const RequestEventManifestsTable = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request: sbomerRequestManifest, loading, error }] = useRequestEventManifest(id!);

  if (error) {
    return <ErrorSection />;
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading data..." />;
  }

  if (!sbomerRequestManifest) {
    return null;
  }

  return (
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Manifests of Generation Request {id}
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
                <pre>{id}</pre>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Received</DescriptionListTerm>
              <DescriptionListDescription>
                <Timestamp date={sbomerRequestManifest.reqReceivalTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                </Timestamp>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Type</DescriptionListTerm>
              <DescriptionListDescription>
                <Label style={{ cursor: 'pointer' }} color="purple">
                  {sbomerRequestManifest.reqEventType}
                </Label>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Request Config</DescriptionListTerm>
              <DescriptionListDescription>
                <Tooltip isContentLeftAligned={true} content={<code>{sbomerRequestManifest.reqConfig}</code>}>
                  <Label>
                    {sbomerRequestManifest.reqConfigTypeName}={sbomerRequestManifest.reqConfigTypeValue}
                  </Label>
                </Tooltip>
              </DescriptionListDescription>
            </DescriptionListGroup>

            <DescriptionListGroup>
              <DescriptionListTerm>Status</DescriptionListTerm>
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
              <DescriptionListTerm>Status Reason</DescriptionListTerm>
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
      <Table aria-label="Manifests table" variant="compact">
        <Caption>Manifests</Caption>
        <Thead>
          <Tr>
            <Th>{columnNames.mId}</Th>
            <Th>{columnNames.mType}</Th>
            <Th>{columnNames.mIdentifier}</Th>
            <Th>{columnNames.mCreationTime}</Th>
            <Th>{columnNames.gStatus}</Th>
            <Th>{columnNames.gId}</Th>
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
              <Td dataLabel={columnNames.mType}>
                <pre>{manifest.generation.type}</pre>
              </Td>
              <Td dataLabel={columnNames.mIdentifier}>
                <Tooltip
                  isContentLeftAligned={true}
                  content={
                    <div>
                      <div>
                        <strong>Purl</strong>
                      </div>
                      <div>{manifest.rootPurl}</div>
                    </div>
                  }
                >
                  <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.identifier}</span>
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.mCreationTime}>
                <Timestamp date={manifest.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - new Date(manifest.creationTime).getTime(), false, 'ago')}
                </Timestamp>
              </Td>
              <Td dataLabel={columnNames.gStatus}>
                <Tooltip
                  isContentLeftAligned={true}
                  content={
                    <div>
                      <div>
                        <strong>{manifest.generation.result}</strong>
                      </div>
                      <div>{manifest.generation.reason}</div>
                    </div>
                  }
                >
                  <Label style={{ cursor: 'pointer' }} color={statusToColor(manifest.generation)}>
                    {statusToDescription(manifest.generation)}
                  </Label>
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.gId}>
                <Link 
                    to={`/generations/${manifest.generation.id}`} 
                    style={{ textDecoration: 'none', color: 'blue' }}
                  >
                  <pre>{manifest.generation.id}</pre>
                  </Link>
              </Td>
            </Tr>
          ))} 
        </Tbody>
      </Table>
      </GridItem>
      </Grid>
    
  );
};
