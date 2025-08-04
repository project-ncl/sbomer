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

import { ManifestsQueryType } from '@appV2/types';
import { isManifestsQueryType } from '@appV2/utils/Utils';

import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useManifestsFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const pageIndex = +(searchParams.get('page') || 1);
  const pageSize = +(searchParams.get('pageSize') || 10);

  const setFilters = useCallback(
    (pageIndex: number, pageSize: number) => {
      setSearchParams((params) => {
        if (pageIndex) {
          params.set('page', pageIndex.toString());
        } else {
          params.delete('page');
        }
        if (pageSize) {
          params.set('pageSize', pageSize.toString());
        } else {
          params.delete('pageSize');
        }
        return params;
      });
    },
    [],
  );

  return { pageIndex, pageSize, setFilters };
}
