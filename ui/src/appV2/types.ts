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
  public creationTime: Date;
  public updatedTime?: Date;
  public finishedTime?: Date;
  public request?: object;
  public metadata?: object;

   constructor(payload: any) {
    this.id = payload.id;
    this.status = payload.status;
    this.result = payload.result;
    this.reason = payload.reason;

    this.creationTime = new Date(payload.created);

    this.updatedTime = new Date(payload.updated);
    

    this.finishedTime = payload.finished ? new Date(payload.finished) : undefined;

    this.request = payload.request;
    this.metadata = payload.metadata;
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



export class SbomerEvent {
  public id: string;
  public parent: any;
  public created: Date;
  public updated: Date;
  public finished?: Date;
  public status: string;
  public reason: string;
  public metadata?: object;
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
    this.metadata = payload.metadata;
    this.request = payload.request;
  }
}
export type GenerateParams = {
  config: string;
};

export enum ManifestsQueryType {
  NoFilter = 'No filter',
  Purl = 'Purl',
}

export enum RequestsQueryType {
  NoFilter = 'No filter',
  PNCBuild = 'PNC Build',
  ContainerImage = 'Container Image',
  ErrataAdvisory = 'Errata Advisory',
  RequestEvent = 'Request Event',
  PNCAnalysis = 'PNC Analysis',
  PNCOperation = 'PNC Operation',
  ErrataReleaseID = 'Errata Release Id',
  ErrataReleaseFullname = 'Errata Release Fullname',
}

export type SbomerApi = {
  getBaseUrl(): string;
  stats(): Promise<SbomerStats>;
  getLogPaths(generationId: string): Promise<Array<string>>;

  getGenerations(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerGeneration[]; total: number }>;

  getManifests(
    pagination: { pageSize: number; pageIndex: number },
    queryType: ManifestsQueryType,
    query: string,
  ): Promise<{ data: SbomerManifest[]; total: number }>;
  getManifestsForGeneration(generationId: string): Promise<{ data: SbomerManifest[]; total: number }>;

  getGeneration(id: string): Promise<SbomerGeneration>;

  getManifest(id: string): Promise<SbomerManifest>;

  getEvents(
    pagination: {
      pageSize: number;
      pageIndex: number;
    },
    queryType: RequestsQueryType,
    query: string,
  ): Promise<{ data: SbomerEvent[]; total: number }>;

  getRequestEvent(id: string): Promise<SbomerEvent>;

  getRequestEventGenerations(id: string): Promise<{ data: SbomerGeneration[]; total: number }>;
};
