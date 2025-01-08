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

export class SbomerRequest {
  public id: string;
  public receivalTime: Date;
  public eventType: string;
  public eventStatus: string;
  public reason: string;
  public requestConfig: string;
  public requestConfigTypeName: string;
  public requestConfigTypeValue: string;
  public event: any;

  constructor(payload: any) {
    this.id = payload.id;
    this.receivalTime = new Date(payload.receivalTime);
    this.eventType = payload.eventType;
    this.eventStatus = payload.eventStatus;
    this.reason = payload.reason;
    this.event = payload.event;

    // Parse `request_config` if it exists
    if (payload.requestConfig) {
      try {
        
        this.requestConfig = JSON.stringify(payload.requestConfig);
        var rConfig = JSON.parse(this.requestConfig); // Convert to JSON object
        this.requestConfigTypeName = rConfig.type;

        // Match the type and extract the value
        switch (this.requestConfigTypeName) {
          case 'errata-advisory':
            this.requestConfigTypeValue = rConfig.advisoryId;
            break;
          case 'image':
            this.requestConfigTypeValue = rConfig.image;
            break;
          case 'pnc-build':
            this.requestConfigTypeValue = rConfig.buildId;
            break;
          case 'pnc-operation':
            this.requestConfigTypeValue = rConfig.operationId;
            break;
          case 'pnc-analysis':
            this.requestConfigTypeValue = rConfig.milestoneId;
            break;
          default:
            this.requestConfigTypeName = '';
            this.requestConfigTypeValue = '';
        }
      } catch (error) {
        console.error('Failed to parse requestConfig:', error);
        this.requestConfig = ''; // Set to null if parsing fails
        this.requestConfigTypeName = '';
        this.requestConfigTypeValue = '';
      }
    } else {
      this.requestConfig = '';
      this.requestConfigTypeName = '';
      this.requestConfigTypeValue = '';
    }
  }
}

export class SbomerRequestManifest {

  public reqId: string;
  public reqReceivalTime: Date;
  public reqEventType: string;
  public reqEventStatus: string;
  public reqReason: string;
  public reqConfig: string;
  public reqConfigTypeName: string;
  public reqConfigTypeValue: string;
  public reqEvent: any;
  public manifests: SbomerManifest[] = [];

  constructor(payload: any) {
    this.reqId = payload.id;
    this.reqReceivalTime = new Date(payload.receivalTime);
    this.reqEventType = payload.eventType;
    this.reqEventStatus = payload.eventStatus;
    this.reqReason = payload.reason;
    this.reqEvent = payload.event;

    // Parse `request_config` if it exists
    if (payload.requestConfig) {
      try {

        this.reqConfig = JSON.stringify(payload.requestConfig);
        var rConfig = JSON.parse(this.reqConfig); // Convert to JSON object
        this.reqConfigTypeName = rConfig.type;

        // Match the type and extract the value
        switch (this.reqConfigTypeName) {
          case 'errata-advisory':
            this.reqConfigTypeValue = rConfig.advisoryId;
            break;
          case 'image':
            this.reqConfigTypeValue = rConfig.image;
            break;
          case 'pnc-build':
            this.reqConfigTypeValue = rConfig.buildId;
            break;
          case 'pnc-operation':
            this.reqConfigTypeValue = rConfig.operationId;
            break;
          case 'pnc-analysis':
            this.reqConfigTypeValue = rConfig.milestoneId;
            break;
          default:
            this.reqConfigTypeName = '';
            this.reqConfigTypeValue = '';
        }
      } catch (error) {
        console.error('Failed to parse requestConfig:', error);
        this.reqConfig = ''; // Set to null if parsing fails
        this.reqConfigTypeName = '';
        this.reqConfigTypeValue = '';
      }
    } else {
      this.reqConfig = '';
      this.reqConfigTypeName = '';
      this.reqConfigTypeValue = '';
    }
    if (payload.manifests) {
      payload.manifests.forEach((man: any) => {
        this.manifests.push(new SbomerManifest(man));
      });
    }
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

  getRequestEvents(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerRequest[]; total: number }>;

  getRequestEvent(id: string): Promise<SbomerRequestManifest>;

  getRequestEventGenerations(id: string): Promise<{ data: SbomerGeneration[]; total: number }>;

};
