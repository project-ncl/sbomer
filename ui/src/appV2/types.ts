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

/** @public */
export type SbomerErrorResponse = {
  errorId: string;
  error: string;
  resource: string;
  message: string;
};

/** @public */
export type SbomerStats = {
  version: string;
  uptime: string;
  uptimeMillis: number;
  messaging: {
    pncConsumer: {
      received: number;
      processed: number;
      skipped: number;
    };
    errataConsumer: {
      received: number;
      processed: number;
      skipped: number;
    };
    producer: {
      nacked: number;
      acked: number;
    };
  };
  resources: {
    generations: {
      total: number;
      inProgress: number;
    };
    manifests: {
      total: number;
    };
  };
};

export class SbomerGeneration {
  public id: string;
  public status: string;
  public result: string;
  public reason: string;
  public created: Date;
  public updated?: Date;
  public finished?: Date;
  public request?: object;
  public metadata?: Map<string, string>;

  constructor(payload: any) {
    this.id = payload.id;
    this.status = payload.status;
    this.result = payload.result;
    this.reason = payload.reason;

    this.created = new Date(payload.created);

    this.updated = payload.updated ? new Date(payload.updated) : undefined;

    this.finished = payload.finished ? new Date(payload.finished) : undefined;

    this.request = payload.request;
    this.metadata = payload.metadata;
  }
}

export class SbomerManifest {
  public id: string;
  public created: Date;

  constructor(payload: any) {
    this.id = payload.id;
    this.created = new Date(payload.created);
  }
}

export class SbomerEvent {
  public id: string;
  public parent: any;
  public created: Date;
  public updated: Date;
  public finished?: Date;
  public status: string;
  public reason: string;
  public metadata?: Map<string, string>;
  public request?: object;
  public generations: SbomerGeneration[] = [];

  constructor(payload: any) {
    this.id = payload.id;
    this.parent = payload.parent;
    this.created = new Date(payload.created);
    this.updated = new Date(payload.updated);
    if (payload.finished) {
      this.finished = new Date(payload.finished);
    }
    this.status = payload.status;
    this.reason = payload.reason;
    this.metadata = payload.metadata ? new Map(Object.entries(payload.metadata)) : undefined;
    this.request = payload.request;
  }
}

export type GenerateParams = {
  config: string;
};

export type SbomerApi = {
  getBaseUrl(): string;
  stats(): Promise<SbomerStats>;
  getLogPaths(generationId: string): Promise<Array<string>>;

  getGenerations(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerGeneration[]; total: number }>;

  getManifests(pagination: { pageSize: number; pageIndex: number }): Promise<{ data: SbomerManifest[]; total: number }>;
  getManifestsForGeneration(generationId: string): Promise<{ data: SbomerManifest[]; total: number }>;

  getGeneration(id: string): Promise<SbomerGeneration>;

  getManifest(id: string): Promise<SbomerManifest>;

  getManifestJson(id: string): Promise<string>;

  getEvents(
    pagination: {
      pageSize: number;
      pageIndex: number;
    },
    query: string,
  ): Promise<{ data: SbomerEvent[]; total: number }>;

  getEvent(id: string): Promise<SbomerEvent>;

  getEventGenerations(id: string): Promise<{ data: SbomerGeneration[]; total: number }>;
};
