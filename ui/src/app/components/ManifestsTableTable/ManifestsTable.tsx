import { timestampToHumanReadable, typeToDescription } from '@app/utils/Utils';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  ToolbarItem,
  Tooltip,
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
import { Link, useNavigate } from 'react-router-dom';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useManifests } from './useSboms';
import { openInNewTab } from '@app/utils/openInNewTab';
import { ManifestsQueryType } from '@app/types';
import { useManifestsFilters } from './useManifestsFilters';
import { NoResultsSection } from '../Sections/NoResultsSection/NoResultSection';

const columnNames = {
  id: 'ID',
  rootPurl: 'Purl',
  type: 'Resource Type',
  identifier: 'Resource Identifier',
  creationTime: 'Created',
};

export const ManifestsTable = () => {

  const navigate = useNavigate();

  const { queryType, queryValue, pageIndex, pageSize, setFilters } = useManifestsFilters();

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
    setFilters(selectValue, searchBarValue, pageIndex, pageSize)
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


  const table = <>
    <Table aria-label="Manifests table" variant="compact">
      <Caption>Latest manifests</Caption>
      <Thead>
        <Tr>
          <Th>{columnNames.id}</Th>
          <Th>{columnNames.type}</Th>
          <Th>{columnNames.identifier}</Th>
          <Th>{columnNames.creationTime}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {value && value.map((manifest) => (
          <Tr
            key={manifest.id}
            isClickable
            onRowClick={() => navigate(`/manifests/${manifest.id}`)}
            onAuxClick={() => openInNewTab(`/manifests/${manifest.id}`)}
          >
            <Td dataLabel={columnNames.id}>
              <Link to={`/manifests/${manifest.id}`}>
                <pre>{manifest.id}</pre>
              </Link>
            </Td>
            <Td dataLabel={columnNames.type}>
              <Label style={{ cursor: 'pointer' }} color="purple">
                {typeToDescription(manifest.generation)}
              </Label>
            </Td>
            <Td dataLabel={columnNames.identifier}>
              <Tooltip
                isContentLeftAligned={true}
                content={
                  <div>
                    <div>
                      <strong>Purl</strong>
                    </div>
                    <div>{manifest.rootPurl}</div>
                  </div>
                }
              >
                <span className="pf-v5-c-timestamp pf-m-help-text">{manifest.identifier}</span>
              </Tooltip>
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
    <Pagination
      itemCount={total}
      widgetId="manifests-table-pagination"
      perPage={pageSize}
      page={pageIndex}
      variant={PaginationVariant.bottom}
      onSetPage={onSetPage}
      onPerPageSelect={onPerPageSelect}
    />
  </>
  const noResults = <NoResultsSection />
  const loadingSkeleton = <Skeleton screenreaderText="Loading data..." />;
  const errorSection = <ErrorSection />

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
    error ? errorSection :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;


  return <>
    {filtersBar}
    {tableArea}
  </>


};
