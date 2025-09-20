import { CodeSnippet, Stack, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@carbon/react';
import React from 'react';

export const SyntaxOperationsTable = () => (
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
        <TableCell><code>:</code></TableCell>
        <TableCell>Exact match</TableCell>
        <TableCell>
          <CodeSnippet type="inline">status:NEW</CodeSnippet>
          <br />
          <CodeSnippet type="inline">metadata.type:build</CodeSnippet>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>:~</code></TableCell>
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
        <TableCell><code>,</code></TableCell>
        <TableCell>OR values</TableCell>
        <TableCell>
          <CodeSnippet type="inline">status:NEW,ERROR</CodeSnippet>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>-</code></TableCell>
        <TableCell>Negate</TableCell>
        <TableCell>
          <CodeSnippet type="inline">-status:PROCESSED</CodeSnippet>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>sort:</code></TableCell>
        <TableCell>Sort</TableCell>
        <TableCell>
          <CodeSnippet type="inline">sort:created:desc</CodeSnippet>
        </TableCell>
      </TableRow>
    </TableBody>
  </Table>
);
