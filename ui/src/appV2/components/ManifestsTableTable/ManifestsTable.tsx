import { useManifests } from '@appV2/components/ManifestsTableTable/useSboms';
import { useManifestsFilters } from '@appV2/components/ManifestsTableTable/useManifestsFilters';
import { ManifestsQueryType } from '@appV2/types';
import { timestampToHumanReadable } from '@appV2/utils/Utils';
import {
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  ToolbarItem,
  Toolbar,
  Select,
  SearchInput,
  SelectOption,
  ToolbarContent,
  SelectList,
  MenuToggleElement,
  MenuToggle,
  SelectGroup,
  Button
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { Link } from 'react-router-dom';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { NoResultsSection } from '@appV2/components/Sections/NoResultsSection/NoResultSection';


const columnNames = {
  id: 'ID',
  creationTime: 'Created',
};

export const ManifestsTable = () => {

  const { queryType, queryValue, pageIndex, pageSize, setFilters } = useManifestsFilters();

  // enable when pagination is implemented
  const enableFiltering = false
  // enable when pagination is implemented
  const enablePagination = false;

  const [searchBarVisible, setSearchBarVisible] = React.useState<boolean>(queryType != ManifestsQueryType.NoFilter);
  const [searchBarValue, setSearchBarValue] = React.useState<string>(queryValue);

  const [selectIsOpen, setSelectIsOpen] = React.useState<boolean>(false);
  const [selectValue, setSelectValue] = React.useState<ManifestsQueryType>(queryType)

  const [isButtonVisible, setButtonVisible] = React.useState<boolean>(queryType != ManifestsQueryType.NoFilter);

  // getting the data and applying the filters sent to the backend here
  const [{ value, loading, total, error }] = useManifests();

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setFilters(queryType, queryValue, newPage, pageSize)
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
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

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={selectIsOpen}
      style={
        {
          width: '200px'
        } as React.CSSProperties
      }
    >
      {selectValue}
    </MenuToggle>
  );

  const select = <Select
    id="single-select"
    isOpen={selectIsOpen}
    selected={selectValue as ManifestsQueryType}
    onSelect={onSelect}
    onOpenChange={(isOpen) => setSelectIsOpen(isOpen)}
    toggle={toggle}
    shouldFocusToggleOnSelect
  >
    <SelectList>
      <SelectGroup>
        <SelectOption value={ManifestsQueryType.NoFilter}>{ManifestsQueryType.NoFilter}</SelectOption>
      </SelectGroup>
      <SelectGroup>
        <SelectOption value={ManifestsQueryType.Purl}>{ManifestsQueryType.Purl}</SelectOption>
      </SelectGroup>
    </SelectList>
  </Select>


  const onChange = (value: string) => {
    setSearchBarValue(value)
  };

  const searchBar = <SearchInput
    placeholder="Enter selected id"
    value={searchBarValue}
    onChange={(_event, value) => onChange(value)}
    onClear={() => onChange('')}
    isAdvancedSearchOpen={!searchBarVisible}
    onKeyDown={(event: React.KeyboardEvent) => {
      if (event.key == 'Enter') {
        onSearchCall();
      }
    }
    }
  />

  const searchButton = <Button
    variant='primary'
    onClick={() => onSearchCall()}>Search</Button>

  const pagination = <Pagination
      itemCount={total}
      widgetId="manifests-table-pagination"
      perPage={pageSize}
      page={pageIndex}
      variant={PaginationVariant.bottom}
      onSetPage={onSetPage}
      onPerPageSelect={onPerPageSelect}
    />


  const table = <>
    <Table aria-label="Manifests table" variant="compact">
      <Caption>Latest manifests</Caption>
      <Thead>
        <Tr>
          <Th>{columnNames.id}</Th>

          <Th>{columnNames.creationTime}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {value && value.map((manifest) => (
          <Tr
            key={manifest.id}
            isClickable
            style={{ cursor: 'auto' }}
          >
            <Td dataLabel={columnNames.id}>
              <Link to={`/manifests/${manifest.id}`}>
                <pre>{manifest.id}</pre>
              </Link>
            </Td>
            <Td dataLabel={columnNames.creationTime}>
              <Timestamp date={manifest.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                {timestampToHumanReadable(Date.now() - manifest.creationTime.getTime(), false, 'ago')}
              </Timestamp>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
    {enablePagination && pagination}
  </>
  const noResults = <NoResultsSection />
  const loadingSkeleton = <Skeleton screenreaderText="Loading data..." />;

  const filtersBar = <>
    <Toolbar>
      <ToolbarContent>
        <ToolbarItem>
          {select}
        </ToolbarItem>
        <ToolbarItem>
          {searchBarVisible && searchBar}
        </ToolbarItem>
        <ToolbarItem>
          {isButtonVisible && searchButton}
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  </>

  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;


  return <>
    {enableFiltering && filtersBar}
    {tableArea}
  </>


};
