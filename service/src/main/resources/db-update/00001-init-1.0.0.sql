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


BEGIN transaction;

CREATE TABLE sbom (
  id bigint  NOT NULL,
  build_id character varying(255)  NOT NULL,
  generation_time timestamp without time zone NOT NULL,
  generator character varying(255)  NOT NULL,
  processor character varying(255)  NULL,
  root_purl character varying(255)  NULL,
  sbom jsonb  NULL,
  status character varying(255)  NOT NULL,
  type character varying(255)  NOT NULL,
  parent_sbom_id bigint  NULL,
  CONSTRAINT sbom_pkey PRIMARY KEY (id),
  CONSTRAINT uq_sbom_buildid_generator UNIQUE (build_id, generator, processor),
  CONSTRAINT fk_sbom_parent_sbom FOREIGN KEY (parent_sbom_id) REFERENCES sbom(id)
);

CREATE INDEX idx_sbom_buildid ON sbom (build_id);

CREATE TABLE db_version (
  id  SERIAL PRIMARY KEY,
  version character varying(20) NOT NULL,
  creation_time timestamp without time zone NOT NULL
);

INSERT INTO db_version(version, creation_time) VALUES ('00001', now());

COMMIT;
