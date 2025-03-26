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
-- Add the new advisory_id information inside the metadata -> properties 
-- Rename some properties with new "redhat:" prefix
--------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_sbom_metadata_with_errata_id()
RETURNS VOID AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT release_metadata ->> 'errata_id' AS errata_id, id  
        FROM sbom 
        WHERE release_metadata IS NOT NULL
    LOOP
        UPDATE sbom
        SET sbom = jsonb_set(
            sbom,
            '{metadata, properties}',
            jsonb_build_array(jsonb_build_object('name', 'redhat:advisory_id', 'value', rec.errata_id)),
            true
        )
        WHERE sbom->'metadata' IS NOT NULL 
        AND id = rec.id;
    END LOOP;

    FOR rec IN
        SELECT r.request_config ->>'advisoryId' AS errata_id, s.id 
        FROM sbom s, sbom_generation_request sr, request r 
        WHERE s.generationrequest_id = sr.id 
        AND sr.request_id = r.id 
        AND r.request_config::text LIKE '%"type": "errata-advisory"%' 
        AND s.sbom::text NOT LIKE '%"name": "redhat:advisory_id"%'
    LOOP
        UPDATE sbom
        SET sbom = jsonb_set(
            sbom,
            '{metadata, properties}',
            jsonb_build_array(jsonb_build_object('name', 'redhat:advisory_id', 'value', rec.errata_id)),
            true
        )
        WHERE sbom->'metadata' IS NOT NULL 
        AND id = rec.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

BEGIN;
    SELECT update_sbom_metadata_with_errata_id();
    INSERT INTO db_version(version, creation_time) VALUES ('00019', now());
COMMIT;
