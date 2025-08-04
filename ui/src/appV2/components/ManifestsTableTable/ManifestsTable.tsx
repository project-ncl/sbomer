import { useManifests } from '@appV2/components/ManifestsTableTable/useSboms';
import { useManifestsFilters } from '@appV2/components/ManifestsTableTable/useManifestsFilters';
import { ManifestsQueryType } from '@appV2/types';
import { timestampToHumanReadable } from '@appV2/utils/Utils';


import React from 'react';
import { Link } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';
import { DataTable, Pagination, SkeletonText, Table, TableBody, TableCell, TableContainer, TableHead, TableHeader, TableRow } from '@carbon/react';


const columnNames = {
  id: 'ID',
  creationTime: 'Created',
};

export const ManifestsTable = () => {

  const { queryType, queryValue, pageIndex, pageSize, setFilters } = useManifestsFilters();

  // enable when pagination is implemented
  const enableFiltering = false
  // enable when pagination is implemented
  const enablePagination = true;

  const [searchBarVisible, setSearchBarVisible] = React.useState<boolean>(queryType != ManifestsQueryType.NoFilter);
  const [searchBarValue, setSearchBarValue] = React.useState<string>(queryValue);

  const [selectIsOpen, setSelectIsOpen] = React.useState<boolean>(false);
  const [selectValue, setSelectValue] = React.useState<ManifestsQueryType>(queryType)

  const [isButtonVisible, setButtonVisible] = React.useState<boolean>(queryType != ManifestsQueryType.NoFilter);

  // getting the data and applying the filters sent to the backend here
  const [{ value, loading, total, error }] = useManifests();

  const onSetPage = (newPage: number) => {
    setFilters(queryType, queryValue, newPage, pageSize)
  };

  const onPerPageSelect = (newPerPage: number) => {
    setFilters(queryType, queryValue, pageIndex, newPerPage)
  };

  const onToggleClick = () => {
    setSelectIsOpen(!selectIsOpen);
  };

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, value: string | number | undefined) => {
    setSelectValue(value as ManifestsQueryType)
    setSelectIsOpen(false);
    switch (value) {
      case ManifestsQueryType.NoFilter:
        setSearchBarVisible(false);
        setButtonVisible(false);
        setSearchBarValue('')
        setFilters(ManifestsQueryType.NoFilter, '', pageIndex, pageSize)
        break;
      default:
        setSearchBarVisible(true);
        setButtonVisible(true);
    }

  };

  const onSearchCall = () => {
    setFilters(selectValue, searchBarValue, 1, pageSize)
  }



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
            onSetPage(page);
            onPerPageSelect(newPageSize);
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
            {rows.map(({ id }) => {
              const manifest = value?.find(m => m.id === id);
              return (
                <TableRow key={id}>
                  <TableCell>
                    <Link to={`/manifests/${id}`}>
                      <pre>{id}</pre>
                    </Link>
                  </TableCell>
                  <TableCell>
                    <span>
                      {manifest?.created
                        ? timestampToHumanReadable(Date.now() - new Date(manifest.created).getTime(), false, 'ago')
                        : ''}
                    </span>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
        {enablePagination && pagination}
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
  </>


};
