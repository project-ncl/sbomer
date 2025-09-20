import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { useRequestEvents } from '@appV2/components/Tables/EventTable/useEvents';
import { useEventsFilters } from '@appV2/components/Tables/EventTable/useEventsFilters';
import { RelativeTimestamp } from '@appV2/components/UtilsComponents/RelativeTimestamp';
import { eventStatusToColor, extractQueryErrorMessageDetails } from '@appV2/utils/Utils';
import {
  Button,
  DataTable,
  DataTableSkeleton,
  Heading,
  Pagination,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarAction,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Tile
} from '@carbon/react';
import { Help } from '@carbon/icons-react';
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

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
  const navigate = useNavigate();
  const { query, pageIndex, pageSize, setFilters } = useEventsFilters();

  const [querySearchbarValue, setQuerySearchbarValue] = React.useState<string>(query || '');

  React.useEffect(() => {
    setQuerySearchbarValue(query || '');
  }, [query]);

  const [{ value, loading, total, error }] = useRequestEvents();

  const onSetPage = (newPage: number) => {
    setFilters(query, newPage, pageSize);
  };

  const onPerPageSelect = (newPerPage: number) => {
    setFilters(query, pageIndex, newPerPage);
  };


  const isQueryValidationError = (error: any) => {
    return error?.message?.includes('The provided query is not valid') ||
      error?.status === 400 ||
      error?.code === 'INVALID_QUERY';
  };

  const clearFilters = () => {
    setQuerySearchbarValue('');
    setFilters('', 1, 10);
  };
  const executeSearch = () => {
    setFilters(querySearchbarValue || '', 1, pageSize);
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

  const querySearchBarValueOnChange = (event: any) => {
    setQuerySearchbarValue(event.target.value);
  };



  if (loading) {
    return loadingSkeleton;
  }

  if (error && !isQueryValidationError(error)) {
    return <ErrorSection title="Could not load events" message={error.message} />;
  }

  const queryErrorTile = error && isQueryValidationError(error) && (() => {
    const { message, details } = extractQueryErrorMessageDetails(error);
    return (
      <Tile>
        <Stack>
          <Heading>Invalid Query</Heading>
          <p>
            {message || 'Your search query is not valid. Please check your syntax or clear filters to try again.'}
          </p>
          {details && <p>{details}</p>}
          <Button kind="primary" size="sm" onClick={clearFilters}>
            Clear filters
          </Button>
        </Stack>
      </Tile>
    );
  })();


  return (
    <DataTable rows={value || []} headers={headers} render={({ rows, headers }) => (
      <TableContainer title="Events" description="Latest events">
        <TableToolbar>
          <TableToolbarContent>
            <TableToolbarSearch
              persistent
              labelText="Search events"
              placeholder="Enter query"
              value={querySearchbarValue}
              onChange={querySearchBarValueOnChange}
              onClear={clearFilters}
              onKeyDown={(event) => { if (event.key === 'Enter') executeSearch(); }}
              size="md"
            />
            <Button
              kind="ghost"
              hasIconOnly
              iconDescription="Query Reference"
              renderIcon={Help}
              onClick={() => navigate("/help")}
            />
          </TableToolbarContent>
        </TableToolbar>

        {queryErrorTile
          ? queryErrorTile
          : value && value.length > 0 ? (
            <>
              <Table>
                <TableHead>
                  <TableRow>
                    {headers.map(header => (
                      <TableHeader key={header.key}>{header.header}</TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {value.map((requestEvent) => (
                    <TableRow key={requestEvent.id}>
                      <TableCell><Link to={`/events/${requestEvent.id}`}><pre>{requestEvent.id}</pre></Link></TableCell>
                      <TableCell><Tag size="md" type={eventStatusToColor(requestEvent.status)}>{requestEvent.status}</Tag></TableCell>
                      <TableCell><RelativeTimestamp date={requestEvent.created} /></TableCell>
                      <TableCell><RelativeTimestamp date={requestEvent.updated} /></TableCell>
                      <TableCell><RelativeTimestamp date={requestEvent.finished} /></TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Pagination
                page={pageIndex}
                pageSize={pageSize}
                pageSizes={[10, 20, 50, 100]}
                totalItems={total || 0}
                onChange={({ page, pageSize: newPageSize }) => {
                  if (page !== pageIndex) onSetPage(page);
                  if (newPageSize !== pageSize) onPerPageSelect(newPageSize);
                }}
              />
            </>
          ) : (
            <NoResultsSection
              title="No events found"
              message="Try adjusting your search query or clear the filters to see all events."
              actionText="Clear filters"
              onActionClick={clearFilters}
            />
          )
        }
      </TableContainer>
    )} />
  );
};
