import {
  Heading,
  UnorderedList,
  ListItem,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  CodeSnippet,
  Grid,
  Column,
  Stack,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Accordion,
  AccordionItem,
} from '@carbon/react';
import React from 'react';

export const HelpPageContent = () => {
  return (
    <Grid>
      <Column sm={4} md={8} lg={12}>
        <Tabs>
          <TabList aria-label="Help page sections">
            <Tab>Query Language</Tab>
            <Tab>About</Tab>
          </TabList>
          <TabPanels>
            <TabPanel>
              <Accordion>
                <AccordionItem title="Generations">
                  <p>This entity does not support the query language at the moment.</p>
                </AccordionItem>

                <AccordionItem title="Manifests">
                  <p>This entity does not support the query language at the moment.</p>
                </AccordionItem>

                <AccordionItem title="Events" open>
                  <Stack gap={7}>
                    <p>
                      Use the search bar to filter and sort events using a simple query language. Combine multiple filters to
                      narrow your results.
                    </p>

                    <Heading>Filtering</Heading>
                    <p>The basic filter format is <CodeSnippet type="inline" hideCopyButton>field:value</CodeSnippet>.</p>

                    <Heading>Searchable Fields</Heading>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>Field</TableHeader>
                          <TableHeader>Description</TableHeader>
                          <TableHeader>Example Value</TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell>id</TableCell>
                          <TableCell>The unique identifier of the event.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>E0AAAAA</CodeSnippet></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>status</TableCell>
                          <TableCell>The current status of the event.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>NEW, PROCESSED, ERROR</CodeSnippet></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>reason</TableCell>
                          <TableCell>The reason text, often used for errors.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>"Processing failed"</CodeSnippet></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>created</TableCell>
                          <TableCell>Timestamp when the event was created.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>2025-09-04</CodeSnippet></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>updated</TableCell>
                          <TableCell>Timestamp when the event was last updated.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>2025-09-04T13:00:00Z</CodeSnippet></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>finished</TableCell>
                          <TableCell>Timestamp when the event processing finished.</TableCell>
                          <TableCell><CodeSnippet type="inline" hideCopyButton>2025</CodeSnippet></TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>

                    <Heading>Operators</Heading>
                    <UnorderedList>
                      <ListItem><CodeSnippet type="inline" hideCopyButton>:</CodeSnippet> Equals (e.g., <CodeSnippet type="inline" hideCopyButton>status:NEW</CodeSnippet>)</ListItem>
                      <ListItem><CodeSnippet type="inline" hideCopyButton>:&gt;</CodeSnippet>, <CodeSnippet type="inline" hideCopyButton>:&gt;=</CodeSnippet>, <CodeSnippet type="inline" hideCopyButton>:&lt;</CodeSnippet>, <CodeSnippet type="inline" hideCopyButton>:&lt;=</CodeSnippet> Greater/Less than (for date fields)</ListItem>
                      <ListItem><CodeSnippet type="inline" hideCopyButton>:~</CodeSnippet> Contains (for string fields like <CodeSnippet type="inline" hideCopyButton>reason</CodeSnippet>)</ListItem>
                      <ListItem><CodeSnippet type="inline" hideCopyButton>,</CodeSnippet> OR for multiple values of the same field (e.g., <CodeSnippet type="inline" hideCopyButton>status:NEW,ERROR</CodeSnippet>)</ListItem>
                      <ListItem><CodeSnippet type="inline" hideCopyButton>-</CodeSnippet> NOT, placed before a filter (e.g., <CodeSnippet type="inline" hideCopyButton>-status:PROCESSED</CodeSnippet>)</ListItem>
                    </UnorderedList>

                    <Heading>Sorting</Heading>
                    <p>
                      Use <CodeSnippet type="inline" hideCopyButton>sort:field</CodeSnippet> or <CodeSnippet type="inline" hideCopyButton>sort:field:direction</CodeSnippet>. Direction can be <CodeSnippet type="inline" hideCopyButton>asc</CodeSnippet> or <CodeSnippet type="inline" hideCopyButton>desc</CodeSnippet>.
                      The default is <CodeSnippet type="inline" hideCopyButton>asc</CodeSnippet>.
                    </p>

                    <Heading>Examples</Heading>
                    <CodeSnippet type="single">status:NEW -reason:~"failed"</CodeSnippet>
                    <CodeSnippet type="single">status:PROCESSED created:&gt;=2025 sort:updated:desc</CodeSnippet>

                  </Stack>
                </AccordionItem>


              </Accordion>
            </TabPanel>

            <TabPanel>
              <Stack gap={7}>
                <Heading>About SBOMer</Heading>
                <p>This is the help page for the SBOMer application.</p>
              </Stack>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </Column>
    </Grid>
  );
};
