import { useManifests } from '@appV2/components/ManifestsTable/useSboms';
import { useManifestsFilters } from '@appV2/components/ManifestsTable/useManifestsFilters';


import React from 'react';
import { Link } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { DataTable, DataTableSkeleton, Pagination, SkeletonText, Table, TableBody, TableCell, TableContainer, TableHead, TableHeader, TableRow } from '@carbon/react';
import RelativeTimestamp from '../UtilsComponents/RelativeTimestamp';


const columnNames = {
  id: 'ID',
  creationTime: 'Created',
};

const headers = [
  { key: 'id', header: columnNames.id },
  { key: 'creationTime', header: columnNames.creationTime },
];

export const ManifestsTable = () => {

  const { pageIndex, pageSize, setFilters } = useManifestsFilters();

  // getting the data and applying the filters sent to the backend here
  const [{ value, loading, total, error }] = useManifests();

  const onSetPage = (newPage: number) => {
    setFilters(newPage, pageSize)
  };

  const onPerPageSelect = (newPerPage: number) => {
    setFilters(pageIndex, newPerPage)
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


  const table = (
    <DataTable
      rows={value || []}
      headers={[
        { key: 'id', header: columnNames.id },
        { key: 'creationTime', header: columnNames.creationTime },
      ]}
      render={({ rows, headers }) => (
        <TableContainer title="Latest manifests">
          <Table aria-label="Manifests Table">
            <TableHead>
              <TableRow>
                {headers.map(header => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {value && value.map((manifest) => {
                return (
                  <TableRow key={manifest.id}>
                    <TableCell>
                      <Link to={`/manifests/${manifest.id}`}>
                        <pre>{manifest.id}</pre>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <RelativeTimestamp date={manifest?.created} />
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
    <TableContainer title="Latest manifests">
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


  return <>
    {tableArea}
  </>
};
