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

import { SbomerGeneration } from '@appV2/types';

type CarbonTagType = 'red' | 'green' | 'blue' | 'gray' | 'magenta' | 'purple' | 'cyan' | 'teal' | 'cool-gray' | 'warm-gray' | 'high-contrast' | 'outline';

const GenerationStatuses = new Map<
  string,
  { description?: string; color: CarbonTagType }
>([
  ['FAILED', { description: 'Failed', color: 'red' }],
  ['GENERATING', { description: 'In progress', color: 'blue' }],
  ['FINISHED', { description: 'Successfully finished', color: 'green' }],
]);

const GenerationResults = new Map<
  string,
  { description?: string; color: CarbonTagType }
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
  { description?: string; color: CarbonTagType }
>([
  ['FAILED', { description: 'Failed', color: 'red' }],
  ['IGNORED', { description: 'Ignored', color: 'gray' }],
  ['IN_PROGRESS', { description: 'In progress', color: 'blue' }],
  ['SUCCESS', { description: 'Successfully finished', color: 'green' }],
  ['NEW', { description: 'New', color: 'teal' }],
  ['PROCESSED', { description: 'Processed', color: 'purple' }],
  ['ERROR', { description: 'Error', color: 'red' }],
  ['INITIALIZED', { description: 'Initialized', color: 'blue' }]
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
  var resolved = GenerationStatuses.get(request.status);

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

  var resolved = GenerationResults.get(request.result);

  return resolved?.description ?? request.result;
}

export function statusToColor(request: SbomerGeneration): CarbonTagType {
  if (!isInProgress(request)) {
    return isSuccess(request) ? 'green' : 'red';
  }

  return 'gray';
}

export function eventStatusToColor(status: string): CarbonTagType {
  var resolved = EventStatuses.get(status);

  return resolved?.color ?? 'gray';
}

export function resultToColor(request: SbomerGeneration): CarbonTagType {
  var resolved = GenerationResults.get(request.result);

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

export function extractQueryErrorMessageDetails(error: any): { message: string; details?: string } {
  if (typeof error?.message === 'string') {
    const match = error.message.match(/response:\s*(['"])(\{.*\})\1/);
    if (match) {
      try {
        const json = JSON.parse(match[2]);
        return {
          message: json.message || 'Unknown error',
          details: Array.isArray(json.details) ? json.details.join(', ') : json.details,
        };
      } catch {
      }
    }
    return { message: error.message };
  }

  if (typeof error?.message === 'object') {
    return {
      message: error.message.message || 'Unknown error',
      details: Array.isArray(error.message.details) ? error.message.details.join(', ') : error.message.details,
    };
  }

  return { message: 'Unknown error' };
}
