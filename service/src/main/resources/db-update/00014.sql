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
-- Create the new 'request' table
----------------------------------------------------------------
BEGIN;

    CREATE TABLE request (
      id character varying(50) NOT NULL,
      receival_time timestamp without time zone NOT NULL,
      event_type character varying(10) NOT NULL,
      request_config jsonb,
      event jsonb NOT NULL,
      CONSTRAINT request_pkey PRIMARY KEY (id)
    );

    CREATE INDEX idx_request_event_type ON request (event_type);

    -- ** HINT1: PostgreSQL only allows creating a GIN index on individual JSONB fields (or JSONB expressions), no multicolumns.
    -- ** HINT2: A single GIN index cannot optimize queries with different keys in the same JSONB document efficiently.
        
    -- We will create a single GIN index on the entire request_config and event fields, and let PostgreSQL optimize searches on any keys.
    CREATE INDEX idx_request_request_config_all ON request USING GIN (request_config);
    CREATE INDEX idx_request_event_all ON request USING GIN (event);

    -- // Another approach is to create a B-Tree index on request_config->'type' and use separate GIN indexes on the specific keys.
    -- // This approach might become very hard to mantain expecially on the 'event' fields, therefore we will not follow this approach.
    -- // However, I will leave the instructions below for future reference
    -- CREATE INDEX idx_request_cfg_type ON request ((request_config->'type'));
    -- CREATE INDEX idx_request_cfg_operationId ON request USING GIN ((request_config->'operationId'));
    -- CREATE INDEX idx_request_cfg_image ON request USING GIN ((request_config->'image'));
    -- CREATE INDEX idx_request_cfg_advisoryId ON request USING GIN ((request_config->'advisoryId'));
    -- CREATE INDEX idx_request_cfg_milestoneId ON request USING GIN ((request_config->'milestoneId'));
    -- CREATE INDEX idx_request_cfg_buildId ON request USING GIN ((request_config->'buildId'));
    
COMMIT;

----------------------------------------------------------------
-- Update the new 'sbom_generation_request' table
----------------------------------------------------------------
BEGIN;
    ALTER TABLE sbom_generation_request ADD COLUMN request_id character varying(50) NULL;
    ALTER TABLE sbom_generation_request ADD CONSTRAINT fk_generationrequest_request FOREIGN KEY (request_id) REFERENCES request(id);
    CREATE INDEX idx_sbom_generation_request_request_id ON sbom_generation_request (request_id);
    
    INSERT INTO db_version(version, creation_time) VALUES ('00014', now());

COMMIT;


 
