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

import { SbomerGeneration } from '@appV2/types';
import { useCallback } from 'react';
import { useAsync } from 'react-use';
import { DefaultSbomerApiV2 } from 'src/appV2/api/DefaultSbomerApiV2';

export function useGenerationLogPaths(request: SbomerGeneration) {
  const sbomerApi = DefaultSbomerApiV2.getInstance();

  const getLogPaths = useCallback(
    async (request: SbomerGeneration) => {
      try {
        return await sbomerApi.getLogPaths(request.id);
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [request.id],
  );

  const {
    loading,
    value: logPaths,
    error,
  } = useAsync(
    () =>
      getLogPaths(request).then((data) => {
        return data;
      }),
    [request.id],
  );

  return [
    {
      logPaths,
      loading,
      error,
    },
  ] as const;
}
