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

import { ManifestsQueryType, RequestsQueryType, SbomerGeneration } from '@appV2/types';
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
  ['GENERATING', { description: 'In progress', color: 'blue' }],
  ['FINISHED', { description: 'Successfully finished', color: 'green' }],
]);

const GenerationRequestResults = new Map<
  string,
  { description?: string; color: React.ComponentProps<typeof Label>['color'] }
>([
  ['ERR_CONFIG_MISSING', { description: 'Missing configuration', color: 'red' }],
  ['ERR_GENERAL', { description: 'General error', color: 'red' }],
  ['ERR_CONFIG_INVALID', { description: 'Invalid configuration', color: 'red' }],
  ['ERR_INDEX_INVALID', { description: 'Invalid product index', color: 'red' }],
  ['ERR_GENERATION', { description: 'Generation failure', color: 'red' }],
  ['ERR_SYSTEM', { description: 'System error', color: 'red' }],
  ['ERR_MULTI', { description: 'Multiple errors', color: 'red' }],
  ['SUCCESS', { description: 'Success', color: 'green' }],
]);

const EventStatuses = new Map<
  string,
  { description?: string; color: React.ComponentProps<typeof Label>['color'] }
>([
  ['FAILED', { description: 'Failed', color: 'red' }],
  ['IGNORED', { description: 'Ignored', color: 'grey' }],
  ['IN_PROGRESS', { description: 'In progress', color: 'blue' }],
  ['SUCCESS', { description: 'Successfully finished', color: 'green' }],
]);

/**
 *
 * @param millis Converts timestamp in milliseconds to relative time in human readable format.
 * @param seconds Decides whether seconds should be displayed.
 * @returns A human readable time.
 */
export function timestampToHumanReadable(millis: number, seconds?: false, suffix?: string): string {
  var secs = millis / 1000;
  var d = Math.floor(secs / 3600 / 24);
  var h = Math.floor((secs - d * 3600 * 24) / 3600);
  var m = Math.floor((secs % 3600) / 60);
  var s = Math.floor((secs % 3600) % 60);

  if (secs < 60) {
    return 'just now';
  }

  var hrd = '';

  if (d > 3) {
    // More than 3 days: only show days
    hrd = d + (d == 1 ? ' day' : ' days');
  } else if (d >= 1) {
    // 1-3 days: show days and hours
    var dDisplay = d + (d == 1 ? ' day' : ' days');
    var hDisplay = h > 0 ? ' ' + h + (h == 1 ? ' hour' : ' hours') : '';
    hrd = dDisplay + hDisplay;
  } else {
    // Less than 1 day: show hours and minutes
    var hDisplay = h > 0 ? h + (h == 1 ? ' hour' : ' hours') : '';
    var mDisplay = m > 0 ? (h > 0 ? ' ' : '') + m + (m == 1 ? ' minute' : ' minutes') : '';
    hrd = hDisplay + mDisplay;

    // If no hours or minutes, show "just now"
    if (!hDisplay && !mDisplay) {
      return 'just now';
    }
  }

  if (seconds) {
    hrd += seconds;
  }

  if (suffix) {
    hrd += ' ' + suffix;
  }

  return hrd;
}



export function statusToDescription(request: SbomerGeneration): string {
  var resolved = GenerationRequestStatuses.get(request.status);

  return resolved?.description ?? request.status;
}

export function eventStatusToDescription(eventStatus: string): string {
  var resolved = EventStatuses.get(eventStatus);

  return resolved?.description ?? eventStatus;
}

export function resultToDescription(request: SbomerGeneration): string {
  if (request.result == null) {
    return 'In progress';
  }

  var resolved = GenerationRequestResults.get(request.result);

  return resolved?.description ?? request.result;
}

export function statusToColor(request: SbomerGeneration): React.ComponentProps<typeof Label>['color'] {
  if (!isInProgress(request)) {
    return isSuccess(request) ? 'green' : 'red';
  }

  return 'grey';
}

export function eventStatusToColor(status: string): React.ComponentProps<typeof Label>['color'] {
  if (status == 'FAILED') {
    return 'red';
  } else if (status == 'IN_PROGRESS') {
    return 'blue';
  } else if (status == 'SUCCESS') {
    return 'green';
  } else {
    return 'grey';
  }
}

export function resultToColor(request: SbomerGeneration): React.ComponentProps<typeof Label>['color'] {
  var resolved = GenerationRequestResults.get(request.result);

  return resolved?.color ?? 'blue';
}

export function isInProgress(request: SbomerGeneration): boolean {
  if (request.status == 'FINISHED' || request.status == 'FAILED') {
    return false;
  }

  return true;
}

export function isSuccess(request: SbomerGeneration): boolean {
  return request.result == 'SUCCESS' ? true : false;
}

export function isManifestsQueryType(value: string): value is ManifestsQueryType {
  return Object.values<string>(ManifestsQueryType).includes(value);
}
export function isRequestsQueryType(value: string): value is RequestsQueryType {
  return Object.values<string>(RequestsQueryType).includes(value);
}
