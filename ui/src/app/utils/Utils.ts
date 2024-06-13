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

import { SbomerGenerationRequest } from '@app/types';
import { Label } from '@patternfly/react-core';

const GenerationRequestTypes = new Map<string, { description?: string }>([
  ['CONTAINERIMAGE', { description: 'Container image' }],
  ['BUILD', { description: 'PNC build' }],
]);

const GenerationRequestStatuses = new Map<
  string,
  { description?: string; color: React.ComponentProps<typeof Label>['color'] }
>([
  ['FAILED', { description: 'Failed', color: 'red' }],
  ['FINISHED', { description: 'Successfully finished', color: 'green' }],
]);

const GenerationRequestResults = new Map<
  string,
  { description?: string; color: React.ComponentProps<typeof Label>['color'] }
>([
  ['ERR_CONFIG_MISSING', { description: 'Missing configuration', color: 'red' }],
  ['ERR_GENERAL', { description: 'General error', color: 'red' }],
  ['ERR_CONFIG_INVALID', { color: 'red' }],
  ['ERR_INDEX_INVALID', { color: 'red' }],
  ['ERR_GENERATION', { color: 'red' }],
  ['ERR_SYSTEM', { color: 'red' }],
  ['ERR_MULTI', { color: 'red' }],
  ['SUCCESS', { description: 'Success', color: 'green' }],
]);

/**
 *
 * @param millis Converts timestamp in milliseconds to relative time in human readable format.
 * @param seconds Decides whether seconds should be displayed.
 * @returns A human readable time.
 */
export function timestampToHumanReadable(millis: number, seconds?: false): string {
  var secs = millis / 1000;
  var d = Math.floor(secs / 3600 / 24);
  var h = Math.floor((secs - d * 3600 * 24) / 3600);
  var m = Math.floor((secs % 3600) / 60);
  var s = Math.floor((secs % 3600) % 60);

  var dDisplay = d > 0 ? d + (d == 1 ? ' day ' : ' days ') : '';
  var hDisplay = h > 0 ? h + (h == 1 ? ' hour ' : ' hours ') : '';
  var mDisplay = m > 0 ? m + (m == 1 ? ' minute ' : ' minutes ') : '';
  var sDisplay = s > 0 ? s + (s == 1 ? ' second' : ' seconds') : '';

  if (seconds) {
    return dDisplay + hDisplay + mDisplay + sDisplay;
  }

  return dDisplay + hDisplay + mDisplay;
}

export function typeToDescription(request: SbomerGenerationRequest): string {
  var resolved = GenerationRequestTypes.get(request.type);

  return resolved?.description ?? request.type;
}

export function statusToDescription(request: SbomerGenerationRequest): string {
  var resolved = GenerationRequestStatuses.get(request.status);

  return resolved?.description ?? request.status;
}

export function resultToDescription(request: SbomerGenerationRequest): string {
  var resolved = GenerationRequestResults.get(request.result);

  return resolved?.description ?? request.result;
}

export function statusToColor(request: SbomerGenerationRequest): React.ComponentProps<typeof Label>['color'] {
  if (!isInProgress(request)) {
    return isSuccess(request) ? 'green' : 'red';
  }

  return 'grey';
}

export function resultToColor(request: SbomerGenerationRequest): React.ComponentProps<typeof Label>['color'] {
  var resolved = GenerationRequestResults.get(request.result);

  return resolved?.color ?? 'blue';
}

export function isInProgress(request: SbomerGenerationRequest): boolean {
  if (request.status == 'FINISHED' || request.status == 'FAILED') {
    return false;
  }

  return true;
}

export function isSuccess(request: SbomerGenerationRequest): boolean {
  return request.result == 'SUCCESS' ? true : false;
}
