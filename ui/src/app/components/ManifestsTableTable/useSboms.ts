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
import { ManifestsQueryType } from '@app/types';
import { useCallback, useState } from 'react';
import useAsyncRetry from 'react-use/lib/useAsyncRetry';

export function useManifests(initialPage: number, intialPageSize: number) {
  const sbomerApi = DefaultSbomerApi.getInstance();
  const [total, setTotal] = useState(0);
  const [pageIndex, setPageIndex] = useState(initialPage || 0);
  const [pageSize, setPageSize] = useState(intialPageSize || 10);
  const [queryType, setQueryType] = useState(ManifestsQueryType.NoFilter);
  const [query, setQuery] = useState('');

  const getManifests = useCallback(
    async ({
      pageSize,
      pageIndex,
      queryType: queryType,
      query,
    }: {
      pageSize: number;
      pageIndex: number;
      queryType: ManifestsQueryType;
      query: string;
    }) => {
      try {
        return await sbomerApi.getManifests({ pageSize, pageIndex }, queryType, query);
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [pageIndex, pageSize, queryType, query],
  );

  const { loading, value, error, retry } = useAsyncRetry(
    () =>
      getManifests({
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
