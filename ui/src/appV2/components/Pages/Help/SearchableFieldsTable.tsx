import { Stack, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@carbon/react';
import React from 'react';

export const SearchableFieldsTable = () => (
  <Table>
    <TableHead>
      <TableRow>
        <TableHeader>Field name</TableHeader>
        <TableHeader>Data Type</TableHeader>
        <TableHeader>Description</TableHeader>
        <TableHeader>Example Values</TableHeader>
      </TableRow>
    </TableHead>
    <TableBody>
      <TableRow>
        <TableCell><code>id</code></TableCell>
        <TableCell>String</TableCell>
        <TableCell>Unique identifier for the record</TableCell>
        <TableCell><code>E0AAAAA</code></TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>status</code></TableCell>
        <TableCell>Enum</TableCell>
        <TableCell>Current processing state of the record</TableCell>
        <TableCell>
          <Stack gap={6}>
            <div></div>
            <code>NEW</code>
            <code>PROCESSED</code>
            <code>ERROR</code>
            <div></div>
          </Stack>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>reason</code></TableCell>
        <TableCell>String</TableCell>
        <TableCell>Human-readable explanation for current status</TableCell>
        <TableCell><code>"Processing failed"</code></TableCell>
      </TableRow>
      <TableRow>
        <TableCell>
          <Stack gap={6}>
            <div></div>
            <code>created</code>
            <code>updated</code>
            <code>finished</code>
            <div></div>
          </Stack>
        </TableCell>
        <TableCell>Date/Timestamp</TableCell>
        <TableCell>
          Record timestamps. Supports partial dates and comparison operators (<code>&gt;</code>, <code>&lt;</code>, <code>&gt;=</code>, <code>&lt;=</code>)
        </TableCell>
        <TableCell>
          <Stack gap={6}>
            <div></div>
            <code>created:&lt;2025-09-05T11:12:37Z</code>
            <code>updated:2025</code>
            <code>finished:&gt;=2025-09-01</code>
            <div></div>
          </Stack>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>metadata.subfield</code></TableCell>
        <TableCell>String</TableCell>
        <TableCell>
          Nested metadata fields accessed using dot notation.
        </TableCell>
        <TableCell>
          <Stack>
            <code>metadata.type:build</code>
          </Stack>
        </TableCell>
      </TableRow>
    </TableBody>
  </Table>
);
