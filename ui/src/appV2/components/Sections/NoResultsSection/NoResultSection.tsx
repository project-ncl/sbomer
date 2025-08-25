import { Button, Heading, Tile, Stack } from '@carbon/react';
import React from 'react';

interface NoResultsSectionProps {
  title: string;
  message: string;
  actionText: string;
  onActionClick: () => void;
}

export const NoResultsSection = ({
  title,
  message,
  actionText,
  onActionClick,
}: NoResultsSectionProps) => (
  <Tile>
    <Stack gap={5}>
      <Heading>{title}</Heading>
      <p>{message}</p>
      <Button kind="primary" onClick={onActionClick}>
        {actionText}
      </Button>
    </Stack>
  </Tile>
);
