import { useRequestEvents } from '@appV2/components/EventTable/useEvents';
import { useEventsFilters } from '@appV2/components/EventTable/useEventsFilters';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import {
  DataTable,
  DataTableSkeleton,
  Pagination,
  Search,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag
} from '@carbon/react';
import React from 'react';
import { Link } from 'react-router-dom';
import RelativeTimestamp from '../UtilsComponents/RelativeTimestamp';

const columnNames = {
  id: 'ID',
  created: 'Created',
  updated: 'Updated',
  finished: 'Finished',
  status: 'Status',
};

const headers = [
  { key: 'id', header: columnNames.id },
  { key: 'status', header: columnNames.status },
  { key: 'created', header: columnNames.created },
  { key: 'updated', header: columnNames.updated },
  { key: 'finished', header: columnNames.finished },
];

export const EventTable = () => {
  const { query, pageIndex, pageSize, setFilters } = useEventsFilters();

  const [querySearchbarValue, setQuerySearchbarValue] = React.useState<string>(query || '');

  const [{ value, loading, total, error }] = useRequestEvents();

  const onSetPage = (newPage: number) => {
    setFilters(query, newPage, pageSize);
  };

  const onPerPageSelect = (newPerPage: number) => {
    setFilters(query, pageIndex, newPerPage);
  };

  const pagination = (
    <Pagination
      backwardText="Previous page"
      forwardText="Next page"
      itemsPerPageText="Items per page:"
      itemRangeText={(min: number, max: number, total: number) => `${min}–${max} of ${total} items`}
      page={pageIndex}
      pageNumberText="Page Number"
      pageSize={pageSize}
      pageSizes={[
        { text: '10', value: 10 },
        { text: '20', value: 20 },
        { text: '50', value: 50 },
        { text: '100', value: 100 },
      ]}
      totalItems={total || 0}
      onChange={({ page, pageSize: newPageSize }) => {
        if (page !== pageIndex) {
          onSetPage(page);
        } else if (newPageSize !== pageSize) {
          onPerPageSelect(newPageSize);
        }
      }}
    />
  );

  const querySearchBarValueOnChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setQuerySearchbarValue(event.target.value);
  };

  const querySearchBar = (
    <div style={{ marginBottom: 'var(--cds-spacing-05)' }}>
      <Search
        labelText="Search events"
        placeholder="Enter query"
        value={querySearchbarValue}
        onChange={querySearchBarValueOnChange}
        onClear={() => setQuerySearchbarValue('')}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            setFilters(querySearchbarValue || '', pageIndex, pageSize);
          }
        }}
        size="lg" />
    </div>
  );

  const table = (
    <DataTable
      rows={value || []}
      headers={[
        { key: 'id', header: columnNames.id },
        { key: 'status', header: columnNames.status },
        { key: 'created', header: columnNames.created },
        { key: 'updated', header: columnNames.updated },
        { key: 'finished', header: columnNames.finished },
      ]}
      render={({ rows, headers }) => (
        <TableContainer title="Latest events">
          <Table aria-label="Events table">
            <TableHead>
              <TableRow>
                {headers.map(header => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {value && value.map((requestEvent) => {
                return (
                  <TableRow key={requestEvent.id}>
                    <TableCell>
                      <Link to={`/events/${requestEvent.id}`}>
                        <pre>{requestEvent.id}</pre>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Tag size='md' >
                        {requestEvent?.status}
                      </Tag>
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={requestEvent?.created} />
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={requestEvent?.updated} />
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={requestEvent?.finished} />
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
          {pagination}
        </TableContainer>
      )}
    />
  );

  const noResults = <NoResultsSection />;
  const loadingSkeleton = (
    <TableContainer title="Latest events">
      <DataTableSkeleton
        columnCount={Object.keys(headers).length}
        showHeader={false}
        showToolbar={false}
        rowCount={10}
      />
      {pagination}
    </TableContainer>
  );

  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;

  return (
    <Stack gap={4}>
      {querySearchBar}
      {tableArea}
    </Stack>
  );
};
