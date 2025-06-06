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
CREATE INDEX idx_metadata_source ON event ((metadata->>'source'));
CREATE INDEX idx_metadata_resolver ON event ((metadata->>'resolver'));
CREATE INDEX idx_metadata_identifier ON event ((metadata->>'identifier'));

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
        identifier varchar(255) not null,
        result varchar(255),
        status varchar(50) not null,
        type varchar(255) not null,
        reason text,
        request jsonb,
        otel_metadata jsonb,
        CONSTRAINT generation_pkey PRIMARY KEY (id)
    );

INSERT INTO
    db_version (version, creation_time)
VALUES
    ('00024', now ());

COMMIT;