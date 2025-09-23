import React from 'react';
import { Heading, Stack, Tile, Tag, Tooltip } from '@carbon/react';
import { Link } from 'react-router-dom';

export interface MetadataOverviewProps {
  metadata?: Map<string, string> | Record<string, string>;
  redirectPrefix: string;
}

export const MetadataOverview: React.FC<MetadataOverviewProps> = ({ metadata, redirectPrefix }) => {
  const metadataEntries = metadata instanceof Map ? Array.from(metadata.entries()) : Object.entries(metadata || {});

  return (
    <Stack gap={5}>
      <Heading>Metadata Overview</Heading>

      <div className="tag-container">
        <Tile>
          {metadataEntries.length > 0 ? (
            metadataEntries.map(([key, value]) => (
              <Tooltip
                key={`${key}-${value}`}
                label={`${key}:${value}`}
                align="top-start"
                enterDelayMs={500}
                className="tag-tooltip"
              >
                <Tag
                  as={Link}
                  size="lg"
                  type="blue"
                  to={`/${redirectPrefix}?query=metadata.${encodeURIComponent(key)}:"${encodeURIComponent(String(value))}"`}
                  className="tag-link"
                  style={{ cursor: 'pointer' }}
                >
                  {key}:{String(value)}
                </Tag>
              </Tooltip>
            ))
          ) : (
            <p>No metadata available</p>
          )}
        </Tile>
      </div>
    </Stack>
  );
};
