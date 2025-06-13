import { ActionList, ActionListItem, Button, Card, CardBody, CardTitle, Grid, GridItem } from '@patternfly/react-core';
import React from 'react';
import { Link } from 'react-router-dom';

export const AboutSection = () => {
  return (
    <Card isLarge>
      <CardTitle size={20}>About</CardTitle>
      <CardBody>
        <Grid cellSpacing={3} span={12}>
          <GridItem span={12}>
            A service to generate <strong>manifests</strong> in the <strong>CycloneDX</strong> format for Application
            Services products.
          </GridItem>
          <GridItem>
            <br />
            <ActionList>
              {/* <ActionListItem>
                <Button variant="primary" size="lg" component={(props: any) => <Link {...props} to="manifests" />}>
                  Manifests
                </Button>
              </ActionListItem> */}
              <ActionListItem>
                <Button variant="primary" size="lg" component={(props: any) => <Link {...props} to="requests" />}>
                  Generation Requests
                </Button>
              </ActionListItem>
              <ActionListItem>
                <Button variant="secondary" size="lg" component={(props: any) => <Link {...props} to="manifests" />}>
                  Manifests
                </Button>
              </ActionListItem>
            </ActionList>
          </GridItem>
        </Grid>
      </CardBody>
    </Card>
  );
};
