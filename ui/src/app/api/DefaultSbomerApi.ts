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

import { GenerateForPncParams, SbomerApi, SbomerGenerationRequest, SbomerStats } from '../types';

type Options = {
  baseUrl: string;
};

export class DefaultSbomerApi implements SbomerApi {
  private readonly baseUrl: string;

  private static _instance: DefaultSbomerApi;

  public static getInstance(): SbomerApi {
    if (!DefaultSbomerApi._instance) {
      var sbomerHost = process.env.REACT_APP_SBOMER_HOST;

      if (!sbomerHost) {
        const url = window.location.href;

        if (url.includes('stage')) {
          sbomerHost = 'sbomer-stage.pnc.engineering.redhat.com';
        } else {
          sbomerHost = 'sbomer.pnc.engineering.redhat.com';
        }
      }

      DefaultSbomerApi._instance = new DefaultSbomerApi({ baseUrl: `https://${sbomerHost}` });
    }

    return DefaultSbomerApi._instance;
  }

  constructor(options: Options) {
    this.baseUrl = options.baseUrl;
  }

  async stats(): Promise<SbomerStats> {
    const response = await fetch(`${this.baseUrl}/api/v1alpha3/stats`);

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching SBOMer statistics, got ' + response.status + " response: '" + body + "'");
    }

    return (await response.json()) as SbomerStats;
  }

  async getGenerationRequests(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerGenerationRequest[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1alpha3/sboms/requests?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error(
        'Failed fetching generation requests from SBOMer, got: ' + response.status + " response: '" + body + "'",
      );
    }

    const data = await response.json();

    const requests: SbomerGenerationRequest[] = [];

    data.content.forEach((request: any) => {
      requests.push(new SbomerGenerationRequest(request));
    });

    return { data: requests, total: data.totalHits };
  }

  async generateForPncBuild({ buildId, config }: GenerateForPncParams): Promise<SbomerGenerationRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1alpha3/sboms/generate/build/${buildId}`, {
      method: 'POST',
      body: config,
      headers: { 'Content-Type': 'application/json' },
    });

    if (response.status != 202) {
      const body = await response.text();

      throw new Error(
        'Failed fetching generation requests from SBOMer, got: ' + response.status + " response: '" + body + "'",
      );
    }

    return response.json() as Promise<SbomerGenerationRequest>;
  }
}
