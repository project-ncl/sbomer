import { SbomerStats } from '@app/types';
import { timestampToHumanReadable } from '@app/utils/Utils';
import {
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  HelperText,
  HelperTextItem,
  Skeleton
} from '@patternfly/react-core';
import { InfoIcon } from '@patternfly/react-icons';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { ErrorSection } from '../ErrorSection/ErrorSection';

const StatisticsContent = (props: { stats: SbomerStats }) => {
  return (
    <>
      <DescriptionList
        columnModifier={{
          default: '2Col',
        }}
      >
        <DescriptionListGroup>
          <DescriptionListTerm>Manifests</DescriptionListTerm>
          <DescriptionListDescription>{props.stats.resources.sboms.total}</DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>Generation requests</DescriptionListTerm>
          <DescriptionListDescription>
            {props.stats.resources.generationRequests.total} ({props.stats.resources.generationRequests.inProgress} in
            progress)
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>Version</DescriptionListTerm>
          <DescriptionListDescription>
            <a href={`https://github.com/project-ncl/sbomer/tree/${props.stats.version}`}>{props.stats.version}</a>
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>Uptime</DescriptionListTerm>
          <DescriptionListDescription>{timestampToHumanReadable(props.stats.uptimeMillis)}</DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>PNC Processed messages</DescriptionListTerm>
          <DescriptionListDescription>
            {props.stats.messaging.pncConsumer.processed} out of {props.stats.messaging.pncConsumer.received} (
            {props.stats.messaging.pncConsumer.received - props.stats.messaging.pncConsumer.processed} failed to process)
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>Errata Processed messages</DescriptionListTerm>
          <DescriptionListDescription>
            {props.stats.messaging.errataConsumer.processed} out of {props.stats.messaging.errataConsumer.received} (
            {props.stats.messaging.errataConsumer.received - props.stats.messaging.errataConsumer.processed} failed to process)
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>Produced messages</DescriptionListTerm>
          <DescriptionListDescription>
            {props.stats.messaging.producer.acked} ({props.stats.messaging.producer.nacked} failed to send)
          </DescriptionListDescription>
        </DescriptionListGroup>
      </DescriptionList>
    </>
  );
};

export const StatsSection = () => {
  const [stats, setStats] = useState<SbomerStats>();
  const [error, setError] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await axios.get('https://sbomer.pnc.engineering.redhat.com/api/v1alpha3/stats');
        setStats(response.data);
      } catch (error) {
        console.error('Error fetching data:', error);
        setError(true);
      }
    };

    fetchData();
  }, []);

  if (error) {
    return <ErrorSection />;
  }

  return stats ? (
    <Card isLarge>
      <CardTitle>Statistics</CardTitle>
      <CardBody>
        <HelperText>
          <HelperTextItem icon={<InfoIcon />}>
            Please note that statistics other than number of manifests and generation requests are currently misleading,
            because these do not take into account values from other nodes located across clusters. This will be
            addressed soon.
          </HelperTextItem>
        </HelperText>
        <br />
        <StatisticsContent stats={stats!} />
      </CardBody>
    </Card>
  ) : (
    <Skeleton screenreaderText="Loading statistics..." />
  );
};
