import { InlineNotification } from '@carbon/react';
import React from 'react';

interface ErrorSectionProps {
  title?: string;
  message?: string;
}

export const ErrorSection = ({
  title = 'Something went wrong',
  message = 'An unexpected error occurred. Please try again.',
}: ErrorSectionProps) => (
  <InlineNotification
    kind="error"
    title={title}
    subtitle={message}
    hideCloseButton
    lowContrast
    role="alert"
  />
);
