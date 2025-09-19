import {
  Accordion,
  AccordionItem,
  CodeSnippet,
  Heading,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TabList,
  TabPanel,
  TabPanels,
  Tabs
} from '@carbon/react';
import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { QueryExample } from './QueryExample';

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
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>Field</TableHeader>
                          <TableHeader>Data Type</TableHeader>
                          <TableHeader>Example Values</TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell><code>id</code></TableCell>
                          <TableCell>String</TableCell>
                          <TableCell><code>E0AAAAA</code></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell><code>status</code></TableCell>
                          <TableCell>Enum</TableCell>
                          <TableCell><Stack orientation='horizontal'>
                            <code>NEW</code><code>PROCESSED</code><code>ERROR</code>
                          </Stack></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell><code>reason</code></TableCell>
                          <TableCell>String</TableCell>
                          <TableCell><code>"Processing failed"</code></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell><code>created</code>, <code>updated</code>, <code>finished</code></TableCell>
                          <TableCell>Date/Timestamp</TableCell>
                          <TableCell><Stack orientation='horizontal'>
                            <code>2025</code><code>2025-09</code><code>2025-09-05</code><code>2025-09-05T11:12:37Z</code></Stack></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell><code>metadata.subfield</code></TableCell>
                          <TableCell>String</TableCell>
                          <TableCell><code>metadata.type:build</code></TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>


                    <Heading>Syntax &amp; Available Operations</Heading>
                    <Table size="sm">
                      <TableHead>
                        <TableRow>
                          <TableHeader>Operator</TableHeader>
                          <TableHeader>Description</TableHeader>
                          <TableHeader>Example</TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell>
                            <code>:</code>
                          </TableCell>
                          <TableCell>Exact match</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">status:NEW</CodeSnippet>
                            <br />
                            <CodeSnippet type="inline">metadata.type:build</CodeSnippet>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>
                            <code>:~</code>
                          </TableCell>
                          <TableCell>Contains</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">reason:~"fail"</CodeSnippet>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>
                            <Stack orientation='horizontal' >
                              <code>:&gt;=</code>
                              <code>:&lt;=</code>
                              <code>:&lt;</code>
                              <code>:&gt;</code>
                            </Stack>
                          </TableCell>
                          <TableCell>Compare (dates)</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">created:&gt;=2025</CodeSnippet>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>
                            <code>,</code>
                          </TableCell>
                          <TableCell>OR values</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">status:NEW,ERROR</CodeSnippet>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>
                            <code>-</code>
                          </TableCell>
                          <TableCell>Negate</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">-status:PROCESSED</CodeSnippet>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>
                            <code>sort:</code>
                          </TableCell>
                          <TableCell>Sort</TableCell>
                          <TableCell>
                            <CodeSnippet type="inline">sort:created:desc</CodeSnippet>
                          </TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
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
