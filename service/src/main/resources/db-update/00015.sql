--
-- JBoss, Home of Professional Open Source.
-- Copyright 2023 Red Hat, Inc., and individual contributors
-- as indicated by the @author tags.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

----------------------------------------------------------------
-- Add the new 'event_status' column to 'request' table
----------------------------------------------------------------
BEGIN;
    ALTER TABLE request ADD COLUMN event_status varchar(255);
    ALTER TABLE request ADD COLUMN reason text;
COMMIT;

----------------------------------------------------------------
-- Script to initialize all the statuses of existing requests
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_request_status()
RETURNS void AS $$
DECLARE
    request_row RECORD;
    in_progress_count INT;
    failed_count INT;
    success_count INT;
    total_count INT;
BEGIN
    -- Loop through all requests
    FOR request_row IN SELECT * FROM request LOOP
        SELECT 
            SUM(CASE WHEN status IN ('NO_OP', 'NEW', 'INITIALIZING', 'INITIALIZED', 'GENERATING')  THEN 1 ELSE 0 END) AS in_progress_count,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
            SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS success_count,
            COUNT(*) AS total_count
        INTO in_progress_count, failed_count, success_count, total_count
        FROM sbom_generation_request
        WHERE request_id = request_row.id;

        -- Update event_status based on the counts
        IF (total_count > 0) THEN
            IF (in_progress_count > 0) THEN
                UPDATE request SET event_status = 'IN_PROGRESS', reason = in_progress_count||'/'||total_count||' in progress'
                WHERE id = request_row.id;
            ELSIF (failed_count > 0) THEN
                UPDATE request SET event_status = 'FAILED', reason = failed_count||'/'||total_count||' failed'
                WHERE id = request_row.id;
            ELSIF (success_count > 0) THEN
                UPDATE request SET event_status = 'SUCCESS', reason = success_count||'/'||total_count||' completed with success'
                WHERE id = request_row.id;
            END IF;
        ELSE
            -- Default case: No related rows found
            UPDATE request SET event_status = 'IGNORED', reason = 'The message type is unknown'
            WHERE id = request_row.id;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

----------------------------------------------------------------
-- Call the function to initialize all the statuses
----------------------------------------------------------------
SELECT update_request_status();


BEGIN;

    -- Initialize all remaining empty values and set the event_status to be not null
    UPDATE request SET event_status = 'SUCCESS' WHERE event_status IS NULL;
    ALTER TABLE request ALTER COLUMN event_status SET NOT NULL;
    CREATE INDEX idx_request_eventstatus ON request (event_status);

    INSERT INTO db_version(version, creation_time) VALUES ('00015', now());

COMMIT;


 
