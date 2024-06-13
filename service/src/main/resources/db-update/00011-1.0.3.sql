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
-- Makes the identifier column taking longer values.
-- Container image names can be long.
-- 
----------------------------------------------------------------
BEGIN transaction;

ALTER TABLE sbom
ALTER COLUMN id TYPE varchar(255);

ALTER TABLE sbom
ALTER COLUMN identifier TYPE varchar(255);

ALTER TABLE sbom
ALTER COLUMN generationrequest_id TYPE varchar(255);

ALTER TABLE sbom_generation_request
ALTER COLUMN id TYPE varchar(255);

ALTER TABLE sbom_generation_request
ALTER COLUMN identifier TYPE varchar(255);

ALTER TABLE sbom_generation_request
ALTER COLUMN status TYPE varchar(255);

INSERT INTO
    db_version (version, creation_time)
VALUES
    ('00011', now ());

COMMIT;