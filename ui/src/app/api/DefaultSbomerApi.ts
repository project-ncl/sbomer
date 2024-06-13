import { GenerateForPncParams, SbomerApi, SbomerGenerationRequest, SbomerStats } from '../types';
import { logger } from '@app/logger';

type Options = {
  baseUrl: string;
};

export class DefaultSbomerApi implements SbomerApi {
  private readonly baseUrl: string;

  private static _instance: DefaultSbomerApi;

  public static getInstance(): SbomerApi {
    if (!DefaultSbomerApi._instance) {
      const sbomerHost = process.env.SBOMER_HOST;

      if (!sbomerHost) {
        logger.error("The 'SBOMER_HOST' environment variable is not set, unable to instantiate SBOMer API client");
      } else {
        DefaultSbomerApi._instance = new DefaultSbomerApi({ baseUrl: `https://${process.env.SBOMER_HOST}` });
      }
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
