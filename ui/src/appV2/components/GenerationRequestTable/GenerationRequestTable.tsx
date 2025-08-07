import { statusToColor, statusToDescription } from '@appV2/utils/Utils';

import React from 'react';
export default RelativeTimestamp;
import { Link, useNavigate } from 'react-router-dom';
import { useSearchParam } from 'react-use';

import { useGenerationRequests } from '@appV2/components/GenerationRequestTable/useGenerationRequests';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { DataTable, TableContainer, Table, TableHead, TableRow, TableHeader, TableBody, TableCell, Tooltip, Tag, SkeletonText, Pagination, DataTableSkeleton } from '@carbon/react';
import RelativeTimestamp from '../UtilsComponents/RelativeTimestamp';
import { SbomerGeneration } from '@appV2/types';

const columnNames = {
  id: 'ID',
  status: 'Status',
  creationTime: 'Created',
  updatedTime: 'Updated',
  finishedTime: 'Finished',
};

const headers = [
  { key: 'id', header: columnNames.id },
  { key: 'status', header: columnNames.status },
  { key: 'creationTime', header: columnNames.creationTime },
  { key: 'updatedTime', header: columnNames.updatedTime },
  { key: 'finishedTime', header: columnNames.finishedTime },
];

export const GenerationRequestTable = () => {
  const navigate = useNavigate();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;


  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize }] = useGenerationRequests(
    +paramPage - 1,
    +paramPageSize,
  );

  const onSetPage = (newPage: number) => {
    setPageIndex(newPage - 1);
    navigate({ search: `?page=${newPage}&pageSize=${pageSize}` });
  };

  const onPerPageSelect = (newPerPage: number) => {
    setPageSize(newPerPage);
    setPageIndex(0);
    navigate({ search: `?page=1&pageSize=${newPerPage}` });
  };

  const pagination = (
    <Pagination
      backwardText="Previous page"
      forwardText="Next page"
      itemsPerPageText="Items per page:"
      itemRangeText={(min: number, max: number, total: number) => `${min}–${max} of ${total} items`}
      page={pageIndex + 1}
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
        if (page !== pageIndex + 1) {
          onSetPage(page);
        } else if (newPageSize !== pageSize) {
          onPerPageSelect(newPageSize);
        }
      }}
    />
  );

  const table = (
    <DataTable
      rows={value || []}
      headers={headers}
      render={({ rows, headers }) => (
        <TableContainer title="Latest manifest generations">
          <Table aria-label="Generation table">
            <TableHead>
              <TableRow>
                {headers.map(header => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {value && value.map((generation: SbomerGeneration) => {
                return (
                  <TableRow key={generation.id}>
                    <TableCell>
                      <Link to={`/generations/${generation.id}`}>
                        <pre>{generation.id}</pre>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Tag size='md' type={statusToColor(generation)}>
                        {generation?.status || 'unknown'}
                      </Tag>
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={generation.created} />
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={generation.updated} />
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={generation.finished} />
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

  const noResults = <NoResultsSection />
  const loadingSkeleton = (
    <TableContainer title="Latest manifest generations">
      <DataTableSkeleton
        columnCount={Object.keys(columnNames).length}
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

  return <>
    {tableArea}
  </>;
};
