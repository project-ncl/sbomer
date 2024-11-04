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
    };
    errataConsumer: {
      received: number;
      processed: number;
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
  public identifier: string;
  public type: string;
  public config: string;
  public envConfig: string;
  public creationTime: Date;

  constructor(payload: any) {
    this.id = payload.id;
    this.status = payload.status;
    this.result = payload.result;
    this.reason = payload.reason;
    this.identifier = payload.identifier;
    this.type = payload.type;
    this.creationTime = new Date(payload.creationTime);
    this.config = payload.config;
    this.envConfig = payload.envConfig;
  }
}

export class SbomerManifest {
  public id: string;
  public identifier: string;
  public rootPurl: string;
  public creationTime: Date;
  public generation: SbomerGeneration;
  public sbom: string;

  constructor(payload: any) {
    this.id = payload.id;
    this.identifier = payload.identifier;
    this.rootPurl = payload.rootPurl;
    this.creationTime = new Date(payload.creationTime);
    this.generation = new SbomerGeneration(payload.generation);
    this.sbom = payload.sbom;
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
};
