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

--------------------------------------------------------------------------
-- Add the Red Hat supplier information inside the metadata 
--------------------------------------------------------------------------

-- Create a dedicated index to find the manifest without metadata->supplier
CREATE INDEX CONCURRENTLY idx_sbom_missing_supplier 
ON sbom USING GIN (sbom jsonb_path_ops) 
WHERE NOT (sbom->'metadata' ? 'supplier');

-- Create a function to find and process manifest in batches of 1000 until there are any
CREATE OR REPLACE PROCEDURE batch_update_supplier()
LANGUAGE plpgsql 
AS $$
DECLARE 
    rows_affected INT;
    batch_count INT := 0;
BEGIN 
    LOOP
        -- Select batch of rows to update
        WITH batch AS (
            SELECT ctid FROM sbom
            WHERE NOT (sbom->'metadata' ? 'supplier')
            LIMIT 1000
        )
        UPDATE sbom
        SET sbom = jsonb_set(
            sbom,
            '{metadata,supplier}',
            '{
                "name": "Red Hat",
                "url": ["https://www.redhat.com"]
            }'::jsonb,
            true
        )
        WHERE ctid IN (SELECT ctid FROM batch);

        -- Get affected rows
        GET DIAGNOSTICS rows_affected = ROW_COUNT;

        -- Log the progress
        RAISE NOTICE 'Batch %: Updated % rows.', batch_count, rows_affected;

        -- Increment batch counter
        batch_count := batch_count + 1;

        -- Stop when no more rows need updates
        EXIT WHEN rows_affected = 0; 

    END LOOP;

    -- Final log message
    RAISE NOTICE 'Update completed. Total batches: %', batch_count;
EXCEPTION
    -- Handle any fatal error and rollback if necessary
    WHEN OTHERS THEN 
        RAISE WARNING 'Error in batch %: %', batch_count, SQLERRM;
        ROLLBACK;
        RETURN;
END $$;

-- Call the function to start updating all the manifests
CALL batch_update_supplier();
   

BEGIN;

   -- Utility to find how many are left to be updated
   -- SELECT COUNT(ID) FROM sbom WHERE NOT (sbom->'metadata' ? 'supplier');

    INSERT INTO db_version(version, creation_time) VALUES ('00020', now());
COMMIT;

