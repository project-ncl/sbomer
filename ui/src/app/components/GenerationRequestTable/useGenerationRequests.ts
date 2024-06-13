import { DefaultSbomerApi } from '@app/api/DefaultSbomerApi';
import { useCallback, useState } from 'react';
import useAsyncRetry from 'react-use/lib/useAsyncRetry';

export function useGenerationRequests() {
  const sbomerApi = DefaultSbomerApi.getInstance();
  const [total, setTotal] = useState(0);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const getGenerationRequests = useCallback(
    async ({ pageSize, pageIndex }: { pageSize: number; pageIndex: number }) => {
      try {
        return await sbomerApi.getGenerationRequests({ pageSize, pageIndex });
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [sbomerApi],
  );

  const { loading, value, error, retry } = useAsyncRetry(
    () =>
      getGenerationRequests({
        pageSize: pageSize,
        pageIndex: pageIndex,
      }).then((data) => {
        setTotal(data.total);
        return data.data;
      }),
    [pageIndex, pageSize, getGenerationRequests],
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
      setTotal,
      retry,
    },
  ] as const;
}
