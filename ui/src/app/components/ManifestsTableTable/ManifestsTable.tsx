import { timestampToHumanReadable } from '@app/utils/Utils';
import {
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { useHistory } from 'react-router-dom';
import { useSearchParam } from 'react-use';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useManifests } from './useSboms';

const columnNames = {
  id: 'ID',
  rootPurl: 'Purl',
  type: 'Resource Type',
  identifier: 'Resource Identifier',
  creationTime: 'Created',
};

export const ManifestsTable = () => {
  const history = useHistory();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;

  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize }] = useManifests(
    +paramPage - 1,
    +paramPageSize,
  );

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPageIndex(newPage - 1);
    history.push({ search: `?page=${newPage}&pageSize=${pageSize}` });
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setPageSize(newPerPage);
    setPageIndex(0);
    history.push({ search: `?page=1&pageSize=${newPerPage}` });
  };

  if (error) {
    return <ErrorSection />;
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading data..." />;
  }

  if (!value) {
    return null;
  }

  return (
    <>
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
          {value.map((manifest) => (
            <Tr key={manifest.id} isClickable onRowClick={() => history.push('/manifests/' + manifest.id)}>
              <Td dataLabel={columnNames.id}>
                <pre>{manifest.id}</pre>
              </Td>
              <Td dataLabel={columnNames.type}>
                <pre>{manifest.generation.type}</pre>
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
        page={pageIndex + 1}
        variant={PaginationVariant.bottom}
        onSetPage={onSetPage}
        onPerPageSelect={onPerPageSelect}
      />
    </>
  );
};
