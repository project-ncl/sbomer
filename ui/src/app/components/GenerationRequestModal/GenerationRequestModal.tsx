import { SbomerGenerationRequest } from '@app/types';
import {
  resultToColor,
  resultToDescription,
  statusToColor,
  statusToDescription,
  timestampToHumanReadable,
  typeToDescription,
} from '@app/utils/Utils';
import { LazyLog } from '@melloware/react-logviewer';
import {
  Button,
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  ExpandableSection,
  ExpandableSectionToggle,
  Label,
  Modal,
  ModalVariant,
  Skeleton,
  Tab,
  TabContent,
  TabTitleText,
  Tabs,
  Text,
  TextContent,
  TextVariants,
  Timestamp,
  TimestampTooltipVariant,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import React, { useState } from 'react';

const Logs = ({ request }: { request: SbomerGenerationRequest }) => {
  const fileNames = ['generate/0/logs/output.log', 'init/logs/output.log'];
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);

  const handleTabClick = (
    event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number,
  ) => {
    setActiveTabKey(tabIndex);
  };

  return (
    <>
      <Title headingLevel="h2">Logs</Title>
      <Tabs id={`${request.id}-logs`} activeKey={activeTabKey} onSelect={handleTabClick} mountOnEnter>
        {fileNames.map((fileName, index) => (
          <Tab eventKey={index} title={<TabTitleText>{fileName}</TabTitleText>}>
            <TabContent id={`log-${index}-${fileName}`}>
              <LazyLog
                loadingComponent={<Skeleton screenreaderText={`Loading ${fileName} log...`} />}
                follow
                caseInsensitive
                enableHotKeys
                enableSearch
                extraLines={1}
                height="520"
                onLineContentClick={function noRefCheck() {}}
                selectableLines
                url={`http://localhost:8080/api/v1alpha3/sbom/requests/${request.id}/logs/${encodeURIComponent(fileName)}`}
              />
            </TabContent>
          </Tab>
        ))}
      </Tabs>
    </>
  );
};

export const GenerationRequestModal = ({ request }: { request: SbomerGenerationRequest }) => {
  const [showModal, setShowModal] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const onToggle = (isExpanded) => {
    setIsExpanded(isExpanded);
  };

  /**
   * Closes the modal and resets the content.
   */
  const closeModal = () => {
    setShowModal(false);
  };

  return (
    <>
      <Modal
        title={`Generation request ${request.id}`}
        variant={ModalVariant.large}
        isOpen={showModal}
        onClose={closeModal}
        actions={[
          <Button key="cancel" variant="link" onClick={closeModal}>
            Close
          </Button>,
        ]}
      >
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
                {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')}
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
              <Tooltip content={request.result}>
                <Label style={{ cursor: 'pointer' }} color={resultToColor(request)}>
                  {resultToDescription(request)}
                </Label>
              </Tooltip>
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Reason</DescriptionListTerm>
            <DescriptionListDescription>{request.reason}</DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
        <br />
        <Logs request={request} />
        <br />
        <CodeBlock>
          <ExpandableSectionToggle
            isExpanded={isExpanded}
            onToggle={onToggle}
            contentId="code-block-expand"
            direction="up"
          >
            <TextContent>
              <Text component={TextVariants.h3}>Generation request raw content</Text>
            </TextContent>
          </ExpandableSectionToggle>
          <CodeBlockCode id={request.id}>
            <ExpandableSection toggleId={request.id} isExpanded={isExpanded} isDetached contentId="code-block-expand">
              {JSON.stringify(request, null, 2)}
            </ExpandableSection>
          </CodeBlockCode>
        </CodeBlock>
      </Modal>

      <Button
        variant="link"
        onClick={() => {
          setShowModal(true);
        }}
      >
        <pre>{request.id}</pre>
      </Button>
    </>
  );
};
