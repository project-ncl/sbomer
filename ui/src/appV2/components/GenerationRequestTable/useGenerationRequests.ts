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

import { useCallback, useState } from 'react';
import useAsyncRetry from 'react-use/lib/useAsyncRetry';
import { DefaultSbomerApiV2 } from 'src/appV2/api/DefaultSbomerApiV2';

export function useGenerationRequests(initialPage: number, intialPageSize: number) {
  const sbomerApi = DefaultSbomerApiV2.getInstance();
  const [total, setTotal] = useState(0);
  const [pageIndex, setPageIndex] = useState(initialPage || 0);
  const [pageSize, setPageSize] = useState(intialPageSize || 10);

  const getGenerations = useCallback(
    async ({ pageSize, pageIndex }: { pageSize: number; pageIndex: number }) => {
      try {
        return await sbomerApi.getGenerations({ pageSize, pageIndex });
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [pageIndex, pageSize],
  );

  const { loading, value, error, retry } = useAsyncRetry(
    () =>
      getGenerations({
        pageSize: pageSize,
        pageIndex: pageIndex,
      }).then((data) => {
        setTotal(data.total);
        return data.data;
      }),
    [pageIndex, pageSize],
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
      retry,
    },
  ] as const;
}
