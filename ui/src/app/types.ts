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
  public requestConfig: string;
  public requestConfigTypeName: string;
  public requestConfigTypeValue: string;
  public event: any;
  public eventDestination: string;

  constructor(payload: any) {
    this.id = payload.id;
    this.receivalTime = new Date(payload.receival_time);
    this.eventType = payload.eventType;
    
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

    // Parse `request_config` if it exists
    if (payload.event) {
      try {
        this.event = JSON.parse(JSON.stringify(payload.event)); // Convert to JSON object
        this.eventDestination = this.event.destination;
      } catch (error) {
        console.error('Failed to parse event:', error);
        this.event = null; // Set to null if parsing fails
        this.eventDestination = '';
      }
    } else {
      this.event = null; // Set to null if parsing fails
      this.eventDestination = '';
    }

    this.event = payload.event;
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
};
