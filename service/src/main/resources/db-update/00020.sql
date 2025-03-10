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
BEGIN;
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
    WHERE sbom->'metadata' ? 'supplier' = false;

    INSERT INTO db_version(version, creation_time) VALUES ('00020', now());
COMMIT;

