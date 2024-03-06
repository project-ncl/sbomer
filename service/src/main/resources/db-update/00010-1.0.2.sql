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
-- Change of column 'buildId' to 'identifier' in tables sbom and sbom_generation_request
-- Addition of column 'type' in sbom_generation_request
-- Update of indexes
--
-- https://issues.redhat.com/browse/SBOMER-37
-- 
----------------------------------------------------------------

BEGIN transaction;

    DROP INDEX idx_sbom_buildid;
    DROP INDEX idx_request_buildid;

    ALTER TABLE sbom RENAME COLUMN build_id TO identifier;
    ALTER TABLE sbom_generation_request RENAME COLUMN build_id TO identifier;
    ALTER TABLE sbom_generation_request ADD COLUMN type character varying(20);

    UPDATE sbom_generation_request SET type = 'BUILD';
    ALTER TABLE sbom_generation_request ALTER COLUMN type SET NOT NULL;

    CREATE INDEX idx_sbom_identifier ON sbom (identifier);
    CREATE INDEX idx_request_identifier ON sbom_generation_request (identifier);
    CREATE INDEX idx_request_type ON sbom_generation_request (type);

COMMIT;

