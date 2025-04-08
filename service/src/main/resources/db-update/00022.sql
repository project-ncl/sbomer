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

----------------------------------------------------
-- Backup script and utilities previously improved 
----------------------------------------------------
-- CREATE INDEX CONCURRENTLY idx_sbom_metadata_property_name
-- ON sbom
-- USING GIN (
--   jsonb_path_query_array(sbom, '$.metadata.properties[*].name') jsonb_path_ops
-- );
-- 
-- CREATE OR REPLACE FUNCTION update_sbom_metadata_with_errata_id_build_manifests()
-- RETURNS VOID AS $$
-- DECLARE
--     rec RECORD;
--     total_to_update INT;
-- BEGIN
-- 
--     SELECT COUNT(s.id) INTO total_to_update
--     FROM sbom s
--     JOIN sbom_generation_request sr ON s.generationrequest_id = sr.id
--     JOIN request r ON sr.request_id = r.id
--     WHERE r.request_config::text LIKE '%"type": "errata-advisory"%'
--       AND NOT (
--         jsonb_path_query_array(s.sbom, '$.metadata.properties[*].name')
--         @> '["redhat:advisory_id"]'
--       );
-- 
--     -- Log total SBOMs to update
--     RAISE NOTICE 'Total SBOMs to update: %', total_to_update;
-- 
--     -- Second loop: from sbom_generation_request/request if advisoryId and no redhat:advisory_id exists
--     FOR rec IN
--         SELECT r.request_config ->> 'advisoryId' AS errata_id, s.id
--         FROM sbom s
--         JOIN sbom_generation_request sr ON s.generationrequest_id = sr.id
--         JOIN request r ON sr.request_id = r.id
--         WHERE r.request_config::text LIKE '%"type": "errata-advisory"%'
--           AND NOT (
--             jsonb_path_query_array(s.sbom, '$.metadata.properties[*].name')
--             @> '["redhat:advisory_id"]'
--           )
--          LIMIT 5000
--     LOOP
--         UPDATE sbom
--         SET sbom = jsonb_set(
--             sbom,
--             '{metadata, properties}',
--             jsonb_build_array(jsonb_build_object('name', 'redhat:advisory_id', 'value', rec.errata_id)),
--             true
--         )
--         WHERE sbom->'metadata' IS NOT NULL 
--         AND id = rec.id;
--     END LOOP;
-- END;
-- $$ LANGUAGE plpgsql;


----------------------------------------------------------------------------------------------------------
-- Create a script which updates all the variants inside imageIndex manifests with the sha-256 algorithm
----------------------------------------------------------------------------------------------------------
-- Create an index to efficiently select the image index manifests
CREATE INDEX CONCURRENTLY idx_sbom_description_prefix
ON sbom ((sbom->'metadata'->'component'->>'description') text_pattern_ops);

-- Create a function to update the variant hashes for all the image index manifests, in batches of 5000
CREATE OR REPLACE FUNCTION update_variant_hashes_from_version()
RETURNS VOID AS $$
DECLARE
    rec RECORD;
    new_sbom JSONB;
    variant JSONB;
    new_variants JSONB := '[]'::jsonb;
    variant_hash JSONB;
    total_to_update INT;
BEGIN

    SELECT COUNT(*) INTO total_to_update
    FROM sbom
    WHERE sbom->'metadata'->'component'->>'description' LIKE 'Image index manifest%';
    
    -- Log total SBOMs to update
    RAISE NOTICE 'Total SBOMs to update: %', total_to_update;

    FOR rec IN
        SELECT id, sbom
        FROM sbom
        WHERE sbom->'metadata'->'component'->>'description' LIKE 'Image index manifest%'
        LIMIT 5000
    LOOP
        new_variants := '[]'::jsonb;

        -- Iterate through the components
        FOR variant IN
            SELECT * FROM jsonb_array_elements(
                rec.sbom #> '{components,0,pedigree,variants}'
            )
        LOOP
            IF variant->>'version' LIKE 'sha256:%' AND (variant->'hashes' IS NULL OR jsonb_array_length(variant->'hashes') = 0) THEN
                -- Extract hash content
                variant_hash := jsonb_build_array(
                    jsonb_build_object(
                        'alg', 'SHA-256',
                        'content', substring(variant->>'version' FROM '^sha256:(.*)$')
                    )
                );
                -- Add the new hashes field
                variant := variant || jsonb_build_object('hashes', variant_hash);
            END IF;

            -- Append the variant back to the updated list
            new_variants := new_variants || jsonb_build_array(variant);
        END LOOP;

        -- Replace the variants array in the original sbom
        new_sbom := jsonb_set(
            rec.sbom,
            '{components,0,pedigree,variants}',
            new_variants
        );

        -- Update the row
        UPDATE sbom
        SET sbom = new_sbom
        WHERE id = rec.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- Call the function to start updating all the manifests. Keep calling until there are no more manifests to update!
CALL update_variant_hashes_from_version();

BEGIN;
    INSERT INTO db_version(version, creation_time) VALUES ('00022', now());
COMMIT;

