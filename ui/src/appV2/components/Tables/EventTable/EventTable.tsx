import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { useRequestEvents } from '@appV2/components/Tables/EventTable/useEvents';
import { useEventsFilters } from '@appV2/components/Tables/EventTable/useEventsFilters';
import { RelativeTimestamp } from '@appV2/components/UtilsComponents/RelativeTimestamp';
import { eventStatusToColor } from '@appV2/utils/Utils';
import {
  DataTable,
  DataTableSkeleton,
  InlineNotification,
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
  TableToolbar,
  TableToolbarSearch,
  Tag,
} from '@carbon/react';
import React from 'react';
import { Link } from 'react-router-dom';

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

  const querySearchBarValueOnChange = (event: any) => {
    setQuerySearchbarValue(event.target.value);
  };


  const isQueryValidationError = (error: any) => {
    return error?.message?.includes('The provided query is not valid') ||
      error?.status === 400 ||
      error?.code === 'INVALID_QUERY';
  };

  const table = (
    <DataTable
      rows={value || []}
      headers={headers}
      render={({ rows, headers }) => (
        <TableContainer title="Events" description="Latest events">
          <TableToolbar>
            <TableToolbarSearch
              labelText="Search events"
              placeholder="Enter query"
              value={querySearchbarValue}
              onChange={querySearchBarValueOnChange}
              onClear={() => {
                setQuerySearchbarValue('');
                setFilters('', pageIndex, pageSize);
              }}
              expanded
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  setFilters(querySearchbarValue || '', pageIndex, pageSize);
                }
              }}
              size="lg"
            />
          </TableToolbar>

          {error && isQueryValidationError(error) ? (
            // query error
            <TableToolbar>
              <InlineNotification
                kind="error"
                title="Invalid Query"
                subtitle={error.message}
                hideCloseButton
                lowContrast
              />
            </TableToolbar>
          ) : error ? (
            <ErrorSection error={error}></ErrorSection>
          ) : null}
          <Table>
            <TableHead>
              <TableRow>
                {headers.map(header => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {value && value.length > 0 ? (
                value.map((requestEvent) => {
                  return (
                    <TableRow key={requestEvent.id}>
                      <TableCell>
                        <Link to={`/events/${requestEvent.id}`}>
                          <pre>{requestEvent.id}</pre>
                        </Link>
                      </TableCell>
                      <TableCell>
                        <Tag size='md' type={eventStatusToColor(requestEvent.status)}>
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
                })
              ) : (
                <>
                  {error ? <TableRow><TableCell colSpan={5}><p>An error occurred</p></TableCell></TableRow> : <TableRow>
                    <TableCell colSpan={5}>
                      <p>No results found</p>
                    </TableCell>
                  </TableRow>}

                </>
              )}
            </TableBody>
          </Table>
          {!error && pagination}
        </TableContainer>
      )}
    />
  );

  const loadingSkeleton = (
    <TableContainer title="Events" description="Latest events">
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
    loading ? loadingSkeleton :
      table;

  return (
    <div className='table-wrapper'>
      <Stack gap={4}>
        {tableArea}
      </Stack>
    </div>
  );
};
