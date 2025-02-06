import { statusToColor, statusToDescription, typeToDescription, timestampToHumanReadable } from '@app/utils/Utils';
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
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useGenerationRequests } from './useGenerationRequests';
import { openInNewTab } from '@app/utils/openInNewTab';

const columnNames = {
  id: 'ID',
  type: 'Type',
  identifier: 'Identifier',
  status: 'Status',
  creationTime: 'Created',
};

export const GenerationRequestTable = () => {
  const navigate = useNavigate();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;

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
      <Table aria-label="Generation request table" variant="compact">
        <Caption>Latest manifest generations</Caption>
        <Thead>
          <Tr>
            <Th>{columnNames.id}</Th>
            <Th>{columnNames.status}</Th>
            <Th>{columnNames.type}</Th>
            <Th>{columnNames.creationTime}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {value.map((generation) => (
            <Tr
              key={generation.id}
              isClickable
              onRowClick={() => navigate('/generations/' + generation.id)}
              onAuxClick={() => openInNewTab('/generations/' + generation.id)}
            >
              <Td dataLabel={columnNames.id}>
                <Link to={'/generations/' + generation.id}>
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
                  <Label style={{ cursor: 'pointer' }} color={statusToColor(generation)}>
                    {statusToDescription(generation)}
                  </Label>

                  {/* <span className="pf-v5-c-timestamp pf-m-help-text">{request.status}</span> */}
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.type}>
                <Tooltip isContentLeftAligned={true} content={<code>{generation.identifier}</code>}>
                  <Label style={{ cursor: 'pointer' }} color="purple">
                    {typeToDescription(generation)}
                  </Label>
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.creationTime}>
                <Timestamp date={generation.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - generation.creationTime.getTime(), false, 'ago')}
                </Timestamp>
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
      <Pagination
        itemCount={total}
        widgetId="request-table-pagination"
        perPage={pageSize}
        page={pageIndex + 1}
        variant={PaginationVariant.bottom}
        onSetPage={onSetPage}
        onPerPageSelect={onPerPageSelect}
      />
    </>
  );
};
