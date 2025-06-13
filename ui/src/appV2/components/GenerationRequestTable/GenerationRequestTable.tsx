import { statusToColor, statusToDescription, timestampToHumanReadable, typeToDescription } from '@appV2/utils/Utils';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSearchParam } from 'react-use';

import { useGenerationRequests } from '@appV2/components/GenerationRequestTable/useGenerationRequests';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';

const columnNames = {
  id: 'ID',
  type: 'Type',
  identifier: 'Identifier',
  status: 'Status',
  creationTime: 'Created',
  updatedTime: 'Updated',
  finishedTime: 'Finished',
};

export const GenerationRequestTable = () => {
  const navigate = useNavigate();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;

  // enable when pagination is implemented
  const paginationEnabled = false;

  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize }] = useGenerationRequests(
    +paramPage - 1,
    +paramPageSize,
  );

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPageIndex(newPage - 1);
    navigate({ search: `?page=${newPage}&pageSize=${pageSize}` });
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setPageSize(newPerPage);
    setPageIndex(0);
    navigate({ search: `?page=1&pageSize=${newPerPage}` });
  };

  const pagination =  <Pagination
      itemCount={total}
      widgetId="request-table-pagination"
      perPage={pageSize}
      page={pageIndex + 1}
      variant={PaginationVariant.bottom}
      onSetPage={onSetPage}
      onPerPageSelect={onPerPageSelect}
    />

  const table = <>
    <Table aria-label="Generation request table" variant="compact">
      <Caption>Latest manifest generations</Caption>
      <Thead>
        <Tr>
          <Th>{columnNames.id}</Th>
          <Th>{columnNames.status}</Th>
          <Th>{columnNames.creationTime}</Th>
          <Th>{columnNames.updatedTime}</Th>
          <Th>{columnNames.finishedTime}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {value && value.map((generation) => (
          <Tr
            key={generation.id}
            isClickable
            style={{ cursor: 'auto' }}
          >
            <Td dataLabel={columnNames.id}>
              <Link to={`/generations/${generation.id}`}>
                <pre>{generation.id}</pre>
              </Link>
            </Td>
            <Td dataLabel={columnNames.status}>
              <Tooltip
                isContentLeftAligned={true}
                content={
                  <div>
                    <div>
                      <strong>{generation.result}</strong>
                    </div>
                    <div>{generation.reason}</div>
                  </div>
                }
              >
                <Label color={statusToColor(generation)}>
                  {statusToDescription(generation)}
                </Label>

                {/* <span className="pf-v5-c-timestamp pf-m-help-text">{request.status}</span> */}
              </Tooltip>
            </Td>
            <Td dataLabel={columnNames.creationTime}>
              <Timestamp date={generation.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                {timestampToHumanReadable(Date.now() - generation.creationTime.getTime(), false, 'ago')}
              </Timestamp>
            </Td>
            <Td dataLabel={columnNames.updatedTime}>
              {generation.updatedTime && (
                <Timestamp date={generation.updatedTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - generation.updatedTime.getTime(), false, 'ago')}
                </Timestamp>
              )}
            </Td>
            <Td dataLabel={columnNames.finishedTime}>
              {generation.finishedTime ? (
                <Timestamp date={generation.finishedTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - generation.finishedTime.getTime(), false, 'ago')}
                </Timestamp>
              ) : (
                <span className="pf-v5-c-timestamp pf-m-help-text">N/A</span>
              )}
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
   {paginationEnabled && pagination}
  </>

  const noResults = <NoResultsSection />
  const loadingSkeleton = <Skeleton screenreaderText="Loading data..." />;

  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;

  return <>
    {tableArea}
  </>;
};
