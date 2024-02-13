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
-- Removal of env_config filed from sbom_generation_request
-- table and move of the content into config field
--
-- https://issues.redhat.com/browse/SBOMER-20
-- 
-- (part of https://issues.redhat.com/browse/SBOMER-14)
----------------------------------------------------------------
BEGIN transaction;

-- Move data from 'env_config' column into 'config' column
WITH
    subquery AS (
        SELECT
            id,
            config || jsonb_build_object ('environment', env_config) AS cfg
        FROM
            sbom_generation_request
    )
UPDATE sbom_generation_request
SET
    config = cfg
FROM
    subquery
WHERE
    sbom_generation_request.id = subquery.id;

-- Drop the unnencessary 'env_config' column
ALTER TABLE sbom_generation_request
DROP COLUMN env_config;

INSERT INTO
    db_version (version, creation_time)
VALUES
    ('00009', now ());

COMMIT;