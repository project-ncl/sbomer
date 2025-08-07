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
import {
  SbomerApi,
  SbomerEvent,
  SbomerGeneration,
  SbomerManifest,
  SbomerStats
} from '../types';

type Options = {
  baseUrl: string;
};

export class DefaultSbomerApiV2 implements SbomerApi {
  private readonly baseUrl: string;

  private client: Axios;
  private static _instance: DefaultSbomerApiV2;

  public static getInstance(): SbomerApi {
    if (!DefaultSbomerApiV2._instance) {
      var sbomerUrl = process.env.REACT_APP_SBOMER_URL;

      if (!sbomerUrl) {
        const url = window.location.href;

        if (url.includes('stage')) {
          sbomerUrl = 'https://sbomer-stage.pnc.engineering.redhat.com';
        } else {
          sbomerUrl = 'https://sbomer.pnc.engineering.redhat.com';
        }
      }

      DefaultSbomerApiV2._instance = new DefaultSbomerApiV2({ baseUrl: sbomerUrl });
    }

    return DefaultSbomerApiV2._instance;
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

  async getManifests(
    pagination: { pageSize: number; pageIndex: number }
  ): Promise<{ data: SbomerManifest[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1beta2/manifests?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching manifests from SBOMer, got: ' + response.status + " response: '" + body + "'");
    }

    const data = await response.json();

    const sboms: SbomerManifest[] = [];

    if (data.content) {
      data.content.forEach((sbom: any) => {
        sboms.push(new SbomerManifest(sbom));
      });
    }else {
    }

    return { data: sboms, total: data.totalHits };
  }

  async getManifestsForGeneration(generationId: string): Promise<{ data: SbomerManifest[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1beta2/manifests?query=generation.id==${generationId}&pageSize=20&pageIndex=0`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching manifests from SBOMer, got: ' + response.status + " response: '" + body + "'");
    }

    const data = await response.json();

    const sboms: SbomerManifest[] = [];

    if (data.content) {
      data.content.forEach((sbom: any) => {
        sboms.push(new SbomerManifest(sbom));
      });
    }

    return { data: sboms, total: data.totalHits };
  }

  async getManifest(id: string): Promise<SbomerManifest> {
    const response = await this.client.get(`/api/v1beta2/manifests/${id}`);
    return new SbomerManifest(response.data);
  }

  async getManifestJson(id: string): Promise<any> {
  const response = await fetch(`${this.baseUrl}/api/v1beta2/manifests/${id}/bom`);
  if (response.status !== 200) {
    const body = await response.text();
    throw new Error('Failed to fetch manifest JSON, got: ' + response.status + " response: '" + body + "'");
  }
  return await response.json();
}

  async getLogPaths(generationId: string): Promise<Array<string>> {
    const response = await this.client.get(`/api/v1beta2/generations/${generationId}/logs`);

    if (response.status != 200) {
      throw new Error(
        'Failed to retrieve log paths for GenerationRequest ' +
          generationId +
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
    const response = await fetch(`${this.baseUrl}/api/v1beta2/stats`);

    if (response.status != 200) {
      const body = await response.text();

      throw new Error('Failed fetching SBOMer statistics, got ' + response.status + " response: '" + body + "'");
    }

    return (await response.json()) as SbomerStats;
  }

  async getGenerations(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerGeneration[]; total: number }> {
    const response = await fetch(
      `${this.baseUrl}/api/v1beta2/generations?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error(
        'Failed fetching generations from SBOMer, got: ' + response.status + " response: '" + body + "'",
      );
    }

    const data = await response.json();

    const requests: SbomerGeneration[] = [];

    if (data.content) {
      data.content.forEach((request: any) => {
        requests.push(new SbomerGeneration(request));
      });
    }

    return { data: requests, total: data.totalHits };
  }

  async getGeneration(id: string): Promise<SbomerGeneration> {
    const response = await this.client.get(`/api/v1beta2/generations/${id}`);
    return new SbomerGeneration(response.data);
  }

  async getEvents(
    pagination: {
      pageSize: number;
      pageIndex: number;
    },
    query: string,
  ): Promise<{ data: SbomerEvent[]; total: number }> {

    const response = await fetch(
      `${this.baseUrl}/api/v1beta2/events/?pageSize=${pagination.pageSize}&pageIndex=${pagination.pageIndex}&query=${encodeURIComponent(query)}`,
    );

    if (response.status != 200) {
      const body = await response.text();

      throw new Error(
        'Failed fetching events from SBOMer, got: ' + response.status + " response: '" + body + "'",
      );
    }

    const data = await response.json();
    data.id
    const requests: SbomerEvent[] = [];


    if (data.content) {
      // basic response without any filters applied
      data.content.forEach((request: any) => {
        requests.push(new SbomerEvent(request));
      });
      return { data: requests, total: data.totalHits };
    }

    return { data: requests, total: requests.length || 0 };
  }

  async getEvent(id: string): Promise<SbomerEvent> {
    const response = await this.client.get(`/api/v1beta2/events/${id}`);
    return new SbomerEvent(response.data);
  }

  async getEventGenerations(id: string): Promise<{ data: SbomerGeneration[]; total: number }> {
    let pageIndex = 0;
    let totalHits = 0;
    const pageSize = 200;
    const requests: SbomerGeneration[] = [];

    // Loop through pages until all content is retrieved
    while (true) {
      const response = await fetch(
        `${this.baseUrl}/api/v1beta2/generations?query=request.id=eq=${id}&sort=creationTime=desc=&pageSize=${pageSize}&pageIndex=${pageIndex}`,
      );

      if (response.status !== 200) {
        const body = await response.text();
        throw new Error(
          'Failed fetching generations from SBOMer, got: ' + response.status + " response: '" + body + "'",
        );
      }

      const data = await response.json();

      // Update totalHits on first page
      if (pageIndex === 0) {
        totalHits = data.totalHits;
      }

      // Add content to the results
      if (data.content) {
        data.content.forEach((request: any) => {
          requests.push(new SbomerGeneration(request));
        });
      }

      // If there is no more content, break the loop
      if (data.pageIndex >= data.totalPages - 1) {
        break;
      }

      // Move to the next page
      pageIndex++;
    }

    return { data: requests, total: totalHits };
  }
}
