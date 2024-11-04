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
import { SbomerGeneration } from '@app/types';
import { useCallback } from 'react';
import { useAsync, useAsyncRetry } from 'react-use';

export function useGenerationRequestManifests(id: string) {
  const sbomerApi = DefaultSbomerApi.getInstance();
  const getManifests = useCallback(
    async (id: string) => {
      try {
        return await sbomerApi.getManifestsForGeneration(id);
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [id],
  );

  const { loading, value, error } = useAsyncRetry(
    () =>
      getManifests(id).then((data) => {
        return data;
      }),
    [getManifests, id],
  );

  return [
    {
      manifests: value?.data || [],
      total: value?.total || 0,
      loading,
      error,
    },
  ] as const;
}
