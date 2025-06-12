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

BEGIN;

CREATE TABLE
    event (
        id character varying(50) NOT NULL,
        created timestamp without time zone NOT NULL,
        updated timestamp without time zone,
        finished timestamp without time zone,
        parent_id character varying(50),
        metadata jsonb NOT NULL,
        request jsonb,
        reason text,
        status varchar(50) not null check (
            status in (
                'NEW',
                'IGNORED',
                'RESOLVING',
                'RESOLVED',
                'IN_PROGRESS',
                'FAILED',
                'SUCCESS',
                'ERROR'
            )
        ),
        CONSTRAINT event_pkey PRIMARY KEY (id)
    );

CREATE INDEX idx_metadata ON event USING GIN (metadata jsonb_path_ops);

-- B-tree indexes on specific metadata content
CREATE INDEX idx_event_metadata_resolver ON event ((metadata->>'resolver'));
CREATE INDEX idx_event_metadata_identifier ON event ((metadata->>'identifier'));

--
-- GIN indexes with pg_trgm (Trigram Extension) for partial matching
--
CREATE INDEX idx_event_metadata_source ON generation USING GIN ((metadata->>'source') gin_trgm_ops);
-- Example: SELECT * FROM event WHERE metadata->>'source' ILIKE 'REST:%';

CREATE TABLE
    event_generation (
        event_id character varying(50) NOT NULL,
        generation_id character varying(50) NOT NULL
    );

CREATE TABLE
    generation (
        id character varying(50) NOT NULL,
        created timestamp without time zone NOT NULL,
        updated timestamp without time zone,
        finished timestamp without time zone,
        parent_id character varying(50),
        result varchar(255),
        status varchar(50) not null,
        reason text,
        request jsonb,
        metadata jsonb,
        CONSTRAINT generation_pkey PRIMARY KEY (id)
    );

--
-- B-tree indexes on specific metadata content
--
CREATE INDEX idx_generation_request_target_type ON generation ((request->'target'->>'type'));
-- Example: SELECT * FROM generation WHERE request->'target'->>'type' = 'CONTAINER_IMAGE';

--
-- GIN indexes with pg_trgm (Trigram Extension) for partial matching
--
CREATE INDEX idx_generation_metadata_deployment ON generation USING GIN ((metadata->>'deployment') gin_trgm_ops);
-- Example: get all generations handled in AWS us-east-1 zone: SELECT * FROM generation WHERE metadata->>'deployment' LIKE '%:%:aws:us-east-1';

CREATE INDEX idx_generation_request_target_identifier ON generation USING GIN ((request->'target'->>'identifier') gin_trgm_ops);
-- Example: All quay.io generations: SELECT * FROM generation WHERE request->'target'->>'identifier' LIKE 'quay.io/%';

CREATE TABLE
    manifest (
        id character varying(50) NOT NULL,
        created timestamp without time zone NOT NULL,
        bom jsonb NOT NULL,
        generation_id character varying(50) NOT NULL
        metadata jsonb,
        CONSTRAINT manifest_pkey PRIMARY KEY (id)
    );

--
-- B-tree indexes on specific metadata content
--
CREATE INDEX idx_manifest_manifest_sha256 ON manifest ((metadata->>'sha256'));

CREATE TABLE
    event_status_history (
        id character varying(50) NOT NULL,
        timestamp timestamp without time zone NOT NULL,
        status varchar(50) not null,
        reason text,
        event_id character varying(50),
        CONSTRAINT event_status_history_pkey PRIMARY KEY (id)
    );

CREATE TABLE
    generation_status_history (
        id character varying(50) NOT NULL,
        timestamp timestamp without time zone NOT NULL,
        status varchar(50) not null,
        reason text,
        generation_id character varying(50),
        CONSTRAINT generation_status_history_pkey PRIMARY KEY (id)
    );


ALTER TABLE IF EXISTS event_generation
    ADD CONSTRAINT fk_event_generation_generation
    foreign key (generation_id) 
    references generation;

ALTER TABLE IF EXISTS event_generation
    ADD CONSTRAINT fk_event_generation_event
    foreign key (event_id) 
    references event;

ALTER TABLE IF EXISTS event_status_history
    ADD CONSTRAINT fk_event_status_history_event
    foreign key (event_id) 
    references event;

ALTER TABLE IF EXISTS generation_status_history
    ADD CONSTRAINT fk_generation_status_history_generation
    foreign key (generation_id) 
    references generation;

ALTER TABLE IF EXISTS generation
    ADD CONSTRAINT fk_generation_parent
    foreign key (parent_id) 
    references generation;

INSERT INTO
    db_version (version, creation_time)
VALUES
    ('00024', now ());

COMMIT;