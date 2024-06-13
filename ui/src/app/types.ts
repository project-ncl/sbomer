/** @public */
export type SbomerStats = {
  version: string;
  uptime: string;
  uptimeMillis: number;
  messaging: {
    consumer: {
      received: number;
      processed: number;
    };
    producer: {
      nacked: number;
      acked: number;
    };
  };
  resources: {
    generationRequests: {
      total: number;
      inProgress: number;
    };
    sboms: {
      total: number;
    };
  };
};

export class SbomerGenerationRequest {
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

export type GenerateForPncParams = {
  buildId: string;
  config?: string;
};

export type SbomerApi = {
  stats(): Promise<SbomerStats>;
  getGenerationRequests(pagination: {
    pageSize: number;
    pageIndex: number;
  }): Promise<{ data: SbomerGenerationRequest[]; total: number }>;

  generateForPncBuild({ buildId, config }: GenerateForPncParams): Promise<SbomerGenerationRequest>;
};
