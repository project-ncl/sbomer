import {
  Accordion,
  AccordionItem,
  CodeSnippet,
  Heading,
  Link,
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
import { useNavigate } from 'react-router-dom';

export const HelpPageContent = () => {
  const navigate = useNavigate();

  // skip the reloading of the Event page
  const handleNavigate = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    navigate('/events');
  };
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
              Use the search bar on the <Link href='/events' onClick={handleNavigate}>Events page</Link> to filter and sort events using a query language.
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
                            <Stack orientation='horizontal'>
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
                    <p>Find all new events</p>
                    <CodeSnippet type="single">status:NEW</CodeSnippet>

                    <p>Find failed events that were not a timeout</p>
                    <CodeSnippet type="single">status:ERROR -reason:~"timeout"</CodeSnippet>

                    <p>Find events processed after the start of 2025</p>
                    <CodeSnippet type="single">finished:&gt;=2025</CodeSnippet>

                    <p>Show new or processed events, sorted by oldest first</p>
                    <CodeSnippet type="single">status:NEW,PROCESSED sort:created:asc</CodeSnippet>

                    <p>Find events updated in September 2025</p>
                    <CodeSnippet type="single">updated:&gt;=2025-09-01 updated:&lt;2025-10-01</CodeSnippet>

                    <p>Find events with a specific ID</p>
                    <CodeSnippet type="single">id:E0AAAAA</CodeSnippet>

                    <p>Find events where the reason contains "network"</p>
                    <CodeSnippet type="single">reason:~"network"</CodeSnippet>

                    <p>Find all events except those with status ERROR</p>
                    <CodeSnippet type="single">-status:ERROR</CodeSnippet>

                    <p>Find events created before September 2025</p>
                    <CodeSnippet type="single">created:&lt;2025-09</CodeSnippet>

                    <p>Sort events by finished date, newest first</p>
                    <CodeSnippet type="single">sort:finished:desc</CodeSnippet>
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
