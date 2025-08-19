import { timestampToHumanReadable } from '@appV2/utils/Utils';
import { Tooltip } from '@carbon/react';
import React from 'react';

interface RelativeTimestampProps {
  /**
   * The date to display as relative time. Can be a Date object or undefined.
   */
  date?: Date;
}

/**
 * Displays a relative timestamp with a tooltip showing the ISO string.
 * Shows "N/A" for missing or invalid dates.
 */
export const RelativeTimestamp: React.FC<RelativeTimestampProps> = ({
  date,
}) => {
  if (!date) {
    return <span>N/A</span>;
  }

  const isoString = date.toISOString();
  const relativeTime = timestampToHumanReadable(
    Date.now() - date.getTime(),
    false,
    'ago'
  );

  return (
    <Tooltip label={isoString}>
      <span>{relativeTime}</span>
    </Tooltip>
  );
};

export default RelativeTimestamp;
