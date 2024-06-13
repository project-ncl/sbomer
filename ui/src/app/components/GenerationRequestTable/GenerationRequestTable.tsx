import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React, { useState } from 'react';
import { useGenerationRequests } from './useGenerationRequests';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { SbomerGenerationRequest } from '@app/types';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
} from '@patternfly/react-core';
import { statusToColor, timestampToHumanReadable } from '@app/utils/Utils';
import { GenerationRequestModal } from '../GenerationRequestModal/GenerationRequestModal';

const columnNames = {
  id: 'ID',
  status: 'Status',
  creationTime: 'Created',
};

export const GenerationRequestTable = () => {
  const [{ pageIndex, pageSize, value, loading, total, error }, { setPageIndex, setPageSize, retry }] =
    useGenerationRequests();

  if (error) {
    return <ErrorSection />;
  }

  if (loading) {
    <Skeleton screenreaderText="Loading data..." />;
  }

  if (!value) {
    return null;
  }

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPageIndex(newPage - 1);
  };

  const onPerPageSelect = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number,
    newPage: number,
  ) => {
    setPageSize(newPerPage);
    setPageIndex(newPage - 1);
  };

  return (
    <>
      <Table aria-label="Generation request table" variant="compact">
        <Caption>Latest manifest generation requests</Caption>
        <Thead>
          <Tr>
            <Th>{columnNames.status}</Th>
            <Th>{columnNames.id}</Th>
            <Th>{columnNames.creationTime}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {value.map((request) => (
            <Tr key={request.id}>
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
                  <Label color={statusToColor(request)}>{request.status}</Label>

                  {/* <span className="pf-v5-c-timestamp pf-m-help-text">{request.status}</span> */}
                </Tooltip>
              </Td>
              <Td dataLabel={columnNames.id}>
                <GenerationRequestModal request={request} />
              </Td>
              <Td dataLabel={columnNames.creationTime}>
                <Timestamp date={request.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {timestampToHumanReadable(Date.now() - request.creationTime.getTime())} ago
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
