import pino from 'pino';

export const logger = pino({
  name: 'sbomer',
  level: process.env.PINO_LOG_LEVEL || 'debug',
  timestamp: pino.stdTimeFunctions.isoTime,
});
