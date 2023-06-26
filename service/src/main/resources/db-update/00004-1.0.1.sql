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
-- Do not delete old tables, but rename and keep a copy of them
-- Need to log in as superuser in DB
----------------------------------------------------------------
BEGIN transaction;

    -- Step 1: Disable foreign key constraints
    ALTER TABLE sbom DISABLE TRIGGER ALL;
    ALTER TABLE sbom_processors DISABLE TRIGGER ALL;

    -- Step 2: Rename tables
    ALTER TABLE sbom RENAME TO old_sbom;
    ALTER TABLE sbom_processors RENAME TO old_sbom_processors;

    -- Step 3: Enable foreign key constraints
    ALTER TABLE old_sbom_processors ENABLE TRIGGER ALL;
    ALTER TABLE old_sbom ENABLE TRIGGER ALL;

COMMIT;

----------------------------------------------------------------
-- Create the new tables
----------------------------------------------------------------

BEGIN transaction;

    CREATE TABLE sbom (
      id character varying(50) NOT NULL,
      build_id character varying(50) NOT NULL,
      root_purl character varying(255) NULL,
      creation_time timestamp without time zone NOT NULL,
      sbom jsonb NULL,     
      status_msg text NULL,
      generationrequest_id character varying(50),
      CONSTRAINT sbom_pkey_ PRIMARY KEY (id)
    );

    CREATE INDEX idx_sbom_buildid ON sbom (build_id);
    CREATE INDEX idx_sbom_rootpurl ON sbom (root_purl);
    CREATE INDEX idx_sbom_generationrequest ON sbom (generationrequest_id);

    CREATE TABLE sbom_generation_request (
      id character varying(50) NOT NULL,
      build_id character varying(50) NOT NULL,
      config jsonb NULL,
      status character varying(20) NOT NULL,
      CONSTRAINT sbom_generation_request_pkey PRIMARY KEY (id)
    );

    CREATE INDEX idx_request_buildid ON sbom_generation_request (build_id);
    CREATE INDEX idx_request_status ON sbom_generation_request (status);

    ALTER TABLE sbom ADD CONSTRAINT fk_sbom_generationrequest FOREIGN KEY (generationrequest_id) REFERENCES sbom_generation_request(id);

    INSERT INTO db_version(version, creation_time) VALUES ('00004', now());

COMMIT;
