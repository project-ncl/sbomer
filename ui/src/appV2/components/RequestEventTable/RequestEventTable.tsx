import { requestEventStatusToColor, requestEventStatusToDescription, timestampToHumanReadable } from '@appV2/utils/Utils';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
  ToolbarContent,
  ToolbarItem,
  Toolbar,
  Select,
  SelectList,
  SelectOption,
  SelectGroup,
  MenuToggle,
  MenuToggleElement,
  SearchInput,
  Button,
  ClipboardCopy,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { Link } from 'react-router-dom';
import { RequestsQueryType } from '@appV2/types';
import { useRequestEventsFilters } from '@appV2/components/RequestEventTable/useRequestEventsFilters';
import { useRequestEvents } from '@appV2/components/RequestEventTable/useRequestEvents';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';


const columnNames = {
  id: 'ID',
  created: 'Created',
  updated: 'Updated',
  finished: 'Finished',
  status: 'Status',
};

export const RequestEventTable = () => {
  const { query, pageIndex, pageSize, setFilters } = useRequestEventsFilters();

  // todo enable when searching is implemented
  const enableSearching = false;
  // enable when pagination is implemented
  const enablePagination = true;



  const [querySearchbarValue, setQuerySearchbarValue] = React.useState<string>();



  const [{ value, loading, total, error },] = useRequestEvents();

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setFilters(query, newPage, pageSize)
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setFilters(query, pageIndex, newPerPage)
  };


  const pagination = <Pagination
    itemCount={total}
    widgetId="request-table-pagination"
    perPage={pageSize}
    page={pageIndex}
    variant={PaginationVariant.bottom}
    onSetPage={onSetPage}
    onPerPageSelect={onPerPageSelect}
  />

  const querySearchBarValueOnChange = (value: string) => {
    setQuerySearchbarValue(value);
  };

  const querySearchBar = <Toolbar>
    <ToolbarItem>
      <SearchInput
        placeholder="Enter query"
        value={querySearchbarValue}
        onChange={(_event, value) => querySearchBarValueOnChange(value)}
        onClear={() => querySearchBarValueOnChange('')}
        onSearch={() => setFilters(querySearchbarValue || '', pageIndex, pageSize)}
        style={{ width: '600px' }}
        >
      </SearchInput>
    </ToolbarItem>
  </Toolbar>

  const table = <>
    <Table aria-label="Events table" variant="compact">
      <Caption>Latest events</Caption>
      <Thead>
        <Tr>
          <Th>{columnNames.id}</Th>
          <Th>{columnNames.status}</Th>
          <Th>{columnNames.created}</Th>
          <Th>{columnNames.updated}</Th>
          <Th>{columnNames.finished}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {value && value.map((requestEvent) => (
          <Tr
            key={requestEvent.id}
            isClickable
            style={{ cursor: 'auto' }}
          >
            <Td dataLabel={columnNames.id}>
              <Link to={`/events/${requestEvent.id}`}>
                <pre>{requestEvent.id}</pre>
              </Link>
            </Td>

            <Td dataLabel={columnNames.status}>
              <Label color="yellow">
                {requestEvent.status}
              </Label>
            </Td>
            <Td dataLabel={columnNames.created}>
              <Timestamp date={requestEvent.created} tooltip={{ variant: TimestampTooltipVariant.default }}>
                {timestampToHumanReadable(Date.now() - requestEvent.created.getTime(), false, 'ago')}
              </Timestamp>
            </Td>
            <Td dataLabel={columnNames.updated}>
              <Timestamp date={requestEvent.updated} tooltip={{ variant: TimestampTooltipVariant.default }}>
                {timestampToHumanReadable(Date.now() - requestEvent.updated.getTime(), false, 'ago')}
              </Timestamp>
            </Td>
            <Td dataLabel={columnNames.finished}>
              {requestEvent.finished ? (
                <Timestamp date={requestEvent.finished} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - requestEvent.finished.getTime(), false, 'ago')}
                </Timestamp>
              ) : (
                <span className="pf-v5-c-timestamp pf-m-help-text">N/A</span>
              )}
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
    {enablePagination && pagination}
  </>
  const noResults = <NoResultsSection />
  const loadingSkeleton = <Skeleton screenreaderText="Loading data..." />;


  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;

  return (
    <>
      {querySearchBar}
      {tableArea}
    </>
  );
};
