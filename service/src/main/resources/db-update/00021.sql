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

-- Create utility indexes to speed up the queries below
CREATE INDEX idx_sbom_components_purl ON sbom USING gin ((sbom->'components') jsonb_path_ops);
CREATE INDEX idx_sbom_metadata_component_purl ON sbom USING gin ((sbom->'metadata'->'component'->'purl') jsonb_path_ops);
CREATE INDEX idx_sbom_components_container ON sbom USING gin ((sbom->'components') jsonb_path_ops) 
WHERE sbom->'components' @> '[{"type": "container"}]';
CREATE INDEX idx_sbom_components_sha256 ON sbom USING gin ((sbom->'components') jsonb_path_ops) 
WHERE sbom->'components' @> '[{"version": "sha256:"}]';


-- Find the number of SBOMs to update
SELECT COUNT(*) 
FROM sbom
WHERE jsonb_typeof(sbom->'components') = 'array'
AND EXISTS (
    SELECT 1 
    FROM jsonb_array_elements(sbom->'components') AS component
    WHERE jsonb_typeof(component) = 'object'
    AND component->>'purl' = sbom->'metadata'->'component'->>'purl'
    AND component->>'type' = 'container'
    AND component->>'version' LIKE 'sha256:%'
    AND (
        jsonb_typeof(component->'hashes') IS DISTINCT FROM 'array'
        OR NOT EXISTS (
            SELECT 1 FROM jsonb_array_elements(component->'hashes') AS h
            WHERE h->>'alg' = 'SHA-256'
        )        
    )        
);


-- Create a function to find and process manifest in batches of 1000 until there are any
CREATE OR REPLACE PROCEDURE batch_update_sha256_hashes()
LANGUAGE plpgsql 
AS $$
DECLARE 
    rows_affected INT;
    batch_count INT := 0;
    total_to_update INT;
BEGIN 
    -- Count how many SBOMs need updates (only when components is an array)
    SELECT COUNT(*) INTO total_to_update
    FROM sbom
    WHERE jsonb_typeof(sbom->'components') = 'array'
    AND EXISTS (
        SELECT 1 
        FROM jsonb_array_elements(sbom->'components') AS component
        WHERE jsonb_typeof(component) = 'object'
        AND component->>'purl' = sbom->'metadata'->'component'->>'purl'
        AND component->>'type' = 'container'
        AND component->>'version' LIKE 'sha256:%'
        AND (
            jsonb_typeof(component->'hashes') IS DISTINCT FROM 'array'
            OR NOT EXISTS (
                SELECT 1 FROM jsonb_array_elements(component->'hashes') AS h
                WHERE h->>'alg' = 'SHA-256'
            )
        )
    );

    -- Log total SBOMs to update
    RAISE NOTICE 'Total SBOMs to update: %', total_to_update;

    LOOP
        -- Select a batch of SBOMs to update
        WITH batch AS (
            SELECT ctid 
            FROM sbom
            WHERE jsonb_typeof(sbom->'components') = 'array'
            AND EXISTS (
                SELECT 1 
                FROM jsonb_array_elements(sbom->'components') AS component
                WHERE jsonb_typeof(component) = 'object'
                AND component->>'purl' = sbom->'metadata'->'component'->>'purl'
                AND component->>'type' = 'container'
                AND component->>'version' LIKE 'sha256:%'
                AND (
                    jsonb_typeof(component->'hashes') IS DISTINCT FROM 'array'
                    OR NOT EXISTS (
                        SELECT 1 FROM jsonb_array_elements(component->'hashes') AS h
                        WHERE h->>'alg' = 'SHA-256'
                    )
                )
            )
            LIMIT 1000
        )
        UPDATE sbom
        SET sbom = jsonb_set(
            sbom, 
            '{components}', 
            (
                SELECT jsonb_agg(
                    CASE 
                        WHEN jsonb_typeof(component) = 'object'
                        AND component->>'purl' = sbom->'metadata'->'component'->>'purl'
                        AND component->>'type' = 'container'
                        AND component->>'version' LIKE 'sha256:%'
                        AND (
                            jsonb_typeof(component->'hashes') IS DISTINCT FROM 'array'
                            OR NOT EXISTS (
                                SELECT 1 FROM jsonb_array_elements(component->'hashes') AS h
                                WHERE h->>'alg' = 'SHA-256'
                            )
                        )
                        THEN component || jsonb_build_object(
                            'hashes', 
                            (CASE 
                                WHEN jsonb_typeof(component->'hashes') = 'array' 
                                THEN component->'hashes' 
                                ELSE '[]'::jsonb
                            END) || jsonb_build_array(
                                jsonb_build_object(
                                    'alg', 'SHA-256',
                                    'content', split_part(component->>'version', ':', 2)
                                )
                            )
                        )
                        ELSE component
                    END
                )
                FROM jsonb_array_elements(sbom->'components') AS component
                WHERE jsonb_typeof(sbom->'components') = 'array'
            )
        )
        WHERE ctid IN (SELECT ctid FROM batch);

        -- Get affected rows
        GET DIAGNOSTICS rows_affected = ROW_COUNT;

        -- Log progress
        batch_count := batch_count + 1;
        RAISE NOTICE 'Batch %: Updated % rows. Remaining: %', batch_count, rows_affected, total_to_update - (batch_count * 1000);

        -- Stop when no more rows need updates
        EXIT WHEN rows_affected = 0; 

    END LOOP;

    -- Final log message
    RAISE NOTICE 'SHA-256 hash update completed. Total batches: %', batch_count;
EXCEPTION
    -- Handle errors and rollback if necessary
    WHEN OTHERS THEN 
        RAISE WARNING 'Error in batch %: %', batch_count, SQLERRM;
        ROLLBACK;
        RETURN;
END $$;

-- Call the function to start updating all the manifests
CALL batch_update_sha256_hashes();
   

BEGIN;
    INSERT INTO db_version(version, creation_time) VALUES ('00021', now());
COMMIT;

