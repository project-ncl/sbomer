import { Button, ButtonSet, Heading, Stack, Tile } from '@carbon/react';
import React from 'react';
import { Link } from 'react-router-dom';

export const AboutSection = () => {
  return (
      <>
          <Heading>About SBOMer</Heading>
          <p>
            A service to generate <strong>manifests</strong> in the <strong>CycloneDX</strong> format for products.
          </p>
        <ButtonSet>
          <Button
            kind="primary"
            as={Link}
            to="generations"
          >
            Generations
          </Button>
          <Button
            kind="secondary"
            as={Link}
            to="manifests"
          >
            Manifests
          </Button>
          <Button
            kind="tertiary"
            as={Link}
            to="events"
          >
            Events
          </Button>
        </ButtonSet>
      </>
  );
};
