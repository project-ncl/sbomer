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
-- Set the type of generation request config accordingly.
----------------------------------------------------------------
BEGIN TRANSACTION;

-- Update 'pnc-build'
WITH
    subquery AS (
        SELECT
            id,
            config || jsonb_build_object ('type', 'pnc-build') AS cfg
        FROM
            sbom_generation_request
        WHERE
            config IS NOT NULL
            AND jsonb_typeof (config) = 'object'
            AND type = 'BUILD'
    )
UPDATE sbom_generation_request
SET
    config = cfg
FROM
    subquery
WHERE
    sbom_generation_request.id = subquery.id;

-- Update 'operation'
WITH
    subquery AS (
        SELECT
            id,
            config || jsonb_build_object ('type', 'operation') AS cfg
        FROM
            sbom_generation_request
        WHERE
            config IS NOT NULL
            AND jsonb_typeof (config) = 'object'
            AND type = 'OPERATION'
    )
UPDATE sbom_generation_request
SET
    config = cfg
FROM
    subquery
WHERE
    sbom_generation_request.id = subquery.id;

-- Update 'syft-image'
WITH
    subquery AS (
        SELECT
            id,
            config || jsonb_build_object ('type', 'syft-image') AS cfg
        FROM
            sbom_generation_request
        WHERE
            config IS NOT NULL
            AND jsonb_typeof (config) = 'object'
            AND type = 'CONTAINERIMAGE'
    )
UPDATE sbom_generation_request
SET
    config = cfg
FROM
    subquery
WHERE
    sbom_generation_request.id = subquery.id;

INSERT INTO
    db_version (version, creation_time)
VALUES
    ('00012', now ());

COMMIT;