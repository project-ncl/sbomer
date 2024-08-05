import { statusToColor, statusToDescription, timestampToHumanReadable } from '@app/utils/Utils';
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
import { useHistory } from 'react-router-dom';
import { useSearchParam } from 'react-use';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useGenerationRequests } from './useGenerationRequests';

const columnNames = {
  id: 'ID',
  type: 'Type',
  identifier: 'Identifier',
  status: 'Status',
  creationTime: 'Created',
};

export const GenerationRequestTable = () => {
  const history = useHistory();
  const paramPage = useSearchParam('page') || 1;
  const paramPageSize = useSearchParam('pageSize') || 10;

  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize }] = useGenerationRequests(
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
      <Table aria-label="Generation request table" variant="compact">
        <Caption>Latest manifest generation requests</Caption>
        <Thead>
          <Tr>
            <Th>{columnNames.status}</Th>
            <Th>{columnNames.type}</Th>
            <Th>{columnNames.id}</Th>
            <Th>{columnNames.creationTime}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {value.map((request) => (
            <Tr key={request.id} isClickable onRowClick={() => history.push('/requests/' + request.id)}>
              <Td dataLabel={columnNames.status}>
                <Tooltip
                  isContentLeftAligned={true}
                  content={
                    <div>
                      <div>
                        <strong>{request.result}</strong>
                      </div>
                      <div>{request.reason}</div>
                    </div>
                  }
                >
                  <Label style={{ cursor: 'pointer' }} color={statusToColor(request)}>
                    {statusToDescription(request)}
                  </Label>

                  {/* <span className="pf-v5-c-timestamp pf-m-help-text">{request.status}</span> */}
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.type}>
                <Tooltip isContentLeftAligned={true} content={<code>{request.identifier}</code>}>
                  <span className="pf-v5-c-timestamp pf-m-help-text">{request.type}</span>
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.id}>
                <pre>{request.id}</pre>
              </Td>
              <Td dataLabel={columnNames.creationTime}>
                <Timestamp date={request.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')}
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
