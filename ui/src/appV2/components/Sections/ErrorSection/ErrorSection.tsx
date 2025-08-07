import { InlineNotification } from '@carbon/react';
import React from 'react';

type NotificationKind = 'error' | 'warning';

interface ErrorSectionProps {
  error: Error;
  kind?: NotificationKind;
}

export const ErrorSection = ({ error, kind = 'error' }: ErrorSectionProps) => {
  const getTitle = (notificationKind: NotificationKind) => {
    return notificationKind === 'error' ? 'An error occurred' : 'Warning';
  };

  return (
    <InlineNotification
      kind={kind}
      title={getTitle(kind)}
      subtitle={error.message}
      hideCloseButton
      lowContrast
    />
  );
};
