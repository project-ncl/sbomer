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
import { useCallback } from 'react';
import { useAsyncRetry } from 'react-use';

export function useManifest(id: string) {
  const sbomerApi = DefaultSbomerApiV2.getInstance();
  const getManifest = useCallback(
    async (id: string) => {
      try {
        return await sbomerApi.getManifest(id);
      } catch (e) {
        return Promise.reject(e);
      }
    },
    [id],
  );

  const {
    loading,
    value: request,
    error,
  } = useAsyncRetry(
    () =>
      getManifest(id).then((data) => {
        return data;
      }),
    [getManifest, id],
  );

  return [
    {
      request,
      loading,
      error,
    },
  ] as const;
}
