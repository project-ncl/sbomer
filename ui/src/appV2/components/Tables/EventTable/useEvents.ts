///
/// JBoss, Home of Professional Open Source.
/// Copyright 2023 Red Hat, Inc., and individual contributors
/// as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
/// http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { DefaultSbomerApiV2 } from '@appV2/api/DefaultSbomerApiV2';
import { useEventsFilters } from '@appV2/components/Tables/EventTable/useEventsFilters';
import { useCallback, useState } from 'react';
import useAsyncRetry from 'react-use/lib/useAsyncRetry';


export function useRequestEvents() {
  const sbomerApi = DefaultSbomerApiV2.getInstance();
  const [total, setTotal] = useState(0);

  const {query, pageIndex, pageSize } = useEventsFilters();

  const getRequestEvents = useCallback(
    async ({
      pageSize,
      pageIndex,
    }: {
      pageSize: number;
      pageIndex: number;
      query: string;
    }) => {
      try {
        const pageIndexOffsetted = pageIndex - 1;
        const response = await sbomerApi.getEvents(
          { pageSize, pageIndex: pageIndexOffsetted },
          query,
        );
        return response;
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [pageIndex, pageSize, query],
  );

  const { loading, value, error, retry } = useAsyncRetry(
    () =>
      getRequestEvents({
        pageSize: +pageSize,
        pageIndex: +pageIndex,
        query: query,
      }).then((data) => {
        setTotal(data.total);
        return data.data;
      }),
    [pageIndex, pageSize, query],
  );

  return [
    {
      total,
      value,
      loading,
      error,
    },
    {
      retry,
    },
  ] as const;
}
