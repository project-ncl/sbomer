import * as React from 'react';
import { CubesIcon } from '@patternfly/react-icons';
import {
  Button,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateVariant,
  PageSection,
  Content,
  ContentVariants,
} from '@patternfly/react-core';

export interface ISupportProps {
  sampleProp?: string;
}

// eslint-disable-next-line prefer-const
const Support: React.FunctionComponent<ISupportProps> = () => (
  <PageSection hasBodyWrapper={false}>
    <EmptyState  headingLevel="h1" icon={CubesIcon}  titleText="Empty State (Stub Support Module)" variant={EmptyStateVariant.full}>
      <EmptyStateBody>
        <Content>
          <Content component="p">
            This represents an the empty state pattern in Patternfly 4. Hopefully it&apos;s simple enough to use but
            flexible enough to meet a variety of needs.
          </Content>
          <Content component={ContentVariants.small}>
            This text has overridden a css component variable to demonstrate how to apply customizations using
            PatternFly&apos;s global variable API.
          </Content>
        </Content>
      </EmptyStateBody><EmptyStateFooter>
      <Button variant="primary">Primary Action</Button>
      <EmptyStateActions>
        <Button variant="link">Multiple</Button>
        <Button variant="link">Action Buttons</Button>
        <Button variant="link">Can</Button>
        <Button variant="link">Go here</Button>
        <Button variant="link">In the secondary</Button>
        <Button variant="link">Action area</Button>
      </EmptyStateActions>
    </EmptyStateFooter></EmptyState>
  </PageSection>
);

export { Support };
