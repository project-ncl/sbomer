import { QueryExample } from '@appV2/components/Pages/Help/QueryExample';
import { SearchableFieldsTable } from '@appV2/components/Pages/Help/SearchableFieldsTable';
import { SyntaxOperationsTable } from '@appV2/components/Pages/Help/SyntaxOperationsTable';
import {
  Accordion,
  AccordionItem,
  Heading,
  Stack,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs
} from '@carbon/react';
import React from 'react';
import { Link as RouterLink } from 'react-router-dom';

export const HelpPageContent = () => {
  return (
    <Stack gap={7}>
      <Stack gap={5}>
        <Heading>Help</Heading>
        <p>
          Welcome to the SBOMer help section. Here you can find documentation and guides
          to help you use the application's features effectively.
        </p>
      </Stack>

      <Accordion>
        <AccordionItem title="Query Language Reference" open>
          <Stack gap={7}>
            <p>
              Use the search bar on the <RouterLink to="/events">Events page</RouterLink> to filter and sort events.
            </p>

            <Tabs>
              <TabList aria-label="List of tabs">
                <Tab>Reference</Tab>
                <Tab>Quick Examples</Tab>
              </TabList>
              <TabPanels>
                <TabPanel>
                  <Stack gap={7}>
                    <Heading>Searchable Fields</Heading>
                    <p>The following fields can be used for filtering and sorting.</p>
                    <SearchableFieldsTable />

                    <Heading>Syntax &amp; Available Operations</Heading>
                    <SyntaxOperationsTable />
                  </Stack>
                </TabPanel>

                <TabPanel>
                  <Stack>
                    <QueryExample description="Find events for a specific metadata type" query="metadata.type:PROCESS" />
                    <QueryExample description="Find all new events" query="status:NEW" />
                    <QueryExample description="Find failed events that were not a timeout" query='status:ERROR -reason:~"timeout"' />
                    <QueryExample description="Find events processed after the start of 2025" query="finished:>=2025" />
                    <QueryExample description="Show new or processed events, sorted by oldest first" query="status:NEW,PROCESSED sort:created:asc" />
                    <QueryExample description="Find events updated in September 2025" query="updated:>=2025-09-01 updated:<2025-10-01" />
                    <QueryExample description="Find events with a specific ID" query="id:E0AAAAA" />
                    <QueryExample description='Find events where the reason contains "network"' query='reason:~"network"' />
                    <QueryExample description="Find all events except those with status ERROR" query="-status:ERROR" />
                    <QueryExample description="Find events created before September 2025" query="created:<2025-09" />
                    <QueryExample description="Sort events by finished date, newest first" query="sort:finished:desc" />
                  </Stack>
                </TabPanel>
              </TabPanels>
            </Tabs>
          </Stack>
        </AccordionItem>
      </Accordion>
    </Stack>
  );
};
