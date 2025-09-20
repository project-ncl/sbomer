import { Stack, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@carbon/react';
import React from 'react';

export const SearchableFieldsTable = () => (
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
        <TableCell>
          <Stack orientation='horizontal'>
            <code>NEW</code><code>PROCESSED</code><code>ERROR</code>
          </Stack>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>reason</code></TableCell>
        <TableCell>String</TableCell>
        <TableCell><code>"Processing failed"</code></TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>created</code>, <code>updated</code>, <code>finished</code></TableCell>
        <TableCell>Date/Timestamp</TableCell>
        <TableCell>
          <Stack orientation='horizontal'>
            <code>2025</code><code>2025-09</code><code>2025-09-05</code><code>2025-09-05T11:12:37Z</code>
          </Stack>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell><code>metadata.subfield</code></TableCell>
        <TableCell>String</TableCell>
        <TableCell><code>metadata.type:build</code></TableCell>
      </TableRow>
    </TableBody>
  </Table>
);
