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

import axios, { Axios, AxiosError } from 'axios';
import { GenerateParams, SbomerApi, SbomerGenerationRequest, SbomerSbom, SbomerStats } from '../types';

type Options = {
  baseUrl: string;
};

export class DefaultSbomerApi implements SbomerApi {
  private readonly baseUrl: string;

  private client: Axios;
  private static _instance: DefaultSbomerApi;

  public static getInstance(): SbomerApi {
    if (!DefaultSbomerApi._instance) {
      var sbomerUrl = process.env.REACT_APP_SBOMER_URL;

      if (!sbomerUrl) {
        const url = window.location.href;

        if (url.includes('stage')) {
          sbomerUrl = 'https://sbomer-stage.pnc.engineering.redhat.com';
        } else {
          sbomerUrl = 'https://sbomer.pnc.engineering.redhat.com';
        }
      }

      DefaultSbomerApi._instance = new DefaultSbomerApi({ baseUrl: sbomerUrl });
    }

    return DefaultSbomerApi._instance;
  }

  public getBaseUrl(): string {
    return this.baseUrl;
  }

  constructor(options: Options) {
    this.baseUrl = options.baseUrl;
    this.client = axios.create({
      baseURL: options.baseUrl,
    });

    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        return Promise.reject(error);
      },
    );
  }

  async getSboms(pagination: { pageSize: number; pageIndex: number }): Promise<{ data: SbomerSbom[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1beta1/sboms?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching manifests from SBOMer, got: ' + response.status + " response: '" + body + "'");
    }

    const data = await response.json();

    const sboms: SbomerSbom[] = [];

    if (data.content) {
      data.content.forEach((sbom: any) => {
        sboms.push(new SbomerSbom(sbom));
      });
    }

    return { data: sboms, total: data.totalHits };
  }

  async getSbomsForRequest(generationRequestId: string): Promise<{ data: SbomerSbom[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1beta1/sboms?query=generationRequest.id==${generationRequestId}&pageSize=20&pageIndex=0`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching manifests from SBOMer, got: ' + response.status + " response: '" + body + "'");
    }

    const data = await response.json();

    const sboms: SbomerSbom[] = [];

    if (data.content) {
      data.content.forEach((sbom: any) => {
        sboms.push(new SbomerSbom(sbom));
      });
    }

    return { data: sboms, total: data.totalHits };
  }

  async getSbom(id: string): Promise<SbomerSbom> {
    const request = await this.client.get<SbomerSbom>(`/api/v1beta1/sboms/${id}`).then((response) => {
      return response.data as SbomerSbom;
    });

    return request;
  }

  async getLogPaths(generationRequestId: string): Promise<Array<string>> {
    const response = await this.client.get(`/api/v1beta1/requests/${generationRequestId}/logs`);

    if (response.status != 200) {
      throw new Error(
        'Failed to retrieve log paths for GenerationRequest ' +
          generationRequestId +
          ', got ' +
          response.status +
          " response: '" +
          response.data +
          "'",
      );
    }
    return response.data as Array<string>;
  }

  async stats(): Promise<SbomerStats> {
    const response = await fetch(`${this.baseUrl}/api/v1beta1/stats`);

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
      `${this.baseUrl}/api/v1beta1/requests?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error(
        'Failed fetching generation requests from SBOMer, got: ' + response.status + " response: '" + body + "'",
      );
    }

    const data = await response.json();

    const requests: SbomerGenerationRequest[] = [];

    if (data.content) {
      data.content.forEach((request: any) => {
        requests.push(new SbomerGenerationRequest(request));
      });
    }

    return { data: requests, total: data.totalHits };
  }

  async getGenerationRequest(id: string): Promise<SbomerGenerationRequest> {
    const request = await this.client
      .get<SbomerGenerationRequest>(`/api/v1beta1/requests/${id}`)
      .then((response) => {
        return response.data as SbomerGenerationRequest;
      });

    return request;
  }

  async generate({ config }: GenerateParams): Promise<Array<SbomerGenerationRequest>> {
    const response = await fetch(`${this.baseUrl}/api/v1beta1/generate`, {
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

    return response.json() as Promise<Array<SbomerGenerationRequest>>;
  }
}
