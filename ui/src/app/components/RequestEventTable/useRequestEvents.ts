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

import { DefaultSbomerApi } from '@app/api/DefaultSbomerApi';
import { RequestsQueryType, SbomerRequest } from '@app/types';
import { useCallback, useState } from 'react';
import useAsyncRetry from 'react-use/lib/useAsyncRetry';

export function useRequestEvents(initialPage: number, intialPageSize: number) {
  const sbomerApi = DefaultSbomerApi.getInstance();
  const [total, setTotal] = useState(0);
  const [pageIndex, setPageIndex] = useState(initialPage || 1);
  const [pageSize, setPageSize] = useState(intialPageSize || 10);
  const [queryType, setQueryType] = useState(RequestsQueryType.NoFilter);
  const [query, setQuery] = useState('');

  const getRequestEvents = useCallback(
    async ({
      pageSize,
      pageIndex,
    }: {
      pageSize: number;
      pageIndex: number;
      queryType: RequestsQueryType;
      query: string;
    }) => {
      try {
        const response = await sbomerApi.getRequestEvents({ pageSize, pageIndex }, queryType, query);
        return response;
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [pageIndex, pageSize, queryType, query],
  );

  const { loading, value, error, retry } = useAsyncRetry(
    () =>
      getRequestEvents({
        pageSize: pageSize,
        pageIndex: pageIndex,
        queryType: queryType,
        query: query,
      }).then((data) => {
        setTotal(data.total);
        return data.data;
      }),
    [pageIndex, pageSize, queryType, query],
  );

  return [
    {
      pageIndex,
      pageSize,
      total,
      value,
      loading,
      error,
    },
    {
      setPageIndex,
      setPageSize,
      setQueryType,
      setQuery,
      retry,
    },
  ] as const;
}
