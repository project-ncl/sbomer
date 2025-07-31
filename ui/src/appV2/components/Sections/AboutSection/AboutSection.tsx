import { Button, Column, Grid, Tile } from '@carbon/react';
import React from 'react';
import { Link } from 'react-router-dom';

export const AboutSection = () => {
  return (
    <Tile>
      <h3>About</h3>
      <p>
        A service to generate <strong>manifests</strong> in the <strong>CycloneDX</strong> format for products.
      </p>
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Button
            kind="primary"
            size="lg"
            as={Link}
            to="generations"
          >
            Generations
          </Button>
          <Button kind="secondary" size="lg" as={Link} to="manifests">
            Manifests
          </Button>
        </Column>
      </Grid>
    </Tile>
  );
};
