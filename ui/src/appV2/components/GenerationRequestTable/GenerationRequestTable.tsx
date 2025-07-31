import { statusToColor, statusToDescription, timestampToHumanReadable } from '@appV2/utils/Utils';

import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSearchParam } from 'react-use';

import { useGenerationRequests } from '@appV2/components/GenerationRequestTable/useGenerationRequests';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { DataTable, TableContainer, Table, TableHead, TableRow, TableHeader, TableBody, TableCell, Tooltip, Tag, SkeletonText, Pagination } from '@carbon/react';

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
  const paginationEnabled = true;

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

    />
  );

  const table = (
    <DataTable
      rows={value || []}
      headers={[
        { key: 'id', header: columnNames.id },
        { key: 'status', header: columnNames.status },
        { key: 'creationTime', header: columnNames.creationTime },
        { key: 'updatedTime', header: columnNames.updatedTime },
        { key: 'finishedTime', header: columnNames.finishedTime },
      ]}
      render={({ rows, headers }) => (
        <TableContainer title="Latest manifest generations">
          <Table aria-label="Generation request table">
            <TableHead>
              <TableRow>
                {headers.map(header => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map(({ id, cells }) => {
                const generation = value?.find(g => g.id === id);
                return (
                  <TableRow key={id}>
                    <TableCell>
                      <Link to={`/generations/${id}`}>
                        <pre>{id}</pre>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Tooltip
                        isContentLeftAligned={true}
                        content={
                          <div>
                            <div>
                              <strong>{generation?.result}</strong>
                            </div>
                            <div>{generation?.reason}</div>
                          </div>
                        }
                      >
                        <Tag>
                          someTag
                        </Tag>
                      </Tooltip>
                    </TableCell>
                    <TableCell>
                      <span>
                        {generation
                          ? timestampToHumanReadable(Date.now() - generation.creationTime.getTime(), false, 'ago')
                          : ''}
                      </span>
                    </TableCell>
                    <TableCell>
                      {generation?.updatedTime && (
                        <span>
                          {timestampToHumanReadable(Date.now() - generation.updatedTime.getTime(), false, 'ago')}
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      {generation?.finishedTime ? (
                        <span>
                          {timestampToHumanReadable(Date.now() - generation.finishedTime.getTime(), false, 'ago')}
                        </span>
                      ) : (
                        <span className="pf-v5-c-timestamp pf-m-help-text">N/A</span>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
          {paginationEnabled && pagination}
        </TableContainer>
      )}
    />
  );

  const noResults = <NoResultsSection />
  const loadingSkeleton = <SkeletonText />;

  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;

  return <>
    {tableArea}
  </>;
};
