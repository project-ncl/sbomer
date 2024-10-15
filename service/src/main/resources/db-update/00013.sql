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
-- Create the new table
----------------------------------------------------------------
BEGIN transaction;

    CREATE TABLE umb_message (
      id bigint NOT NULL,
      consumer character varying(10) NOT NULL,
      receival_time timestamp without time zone NOT NULL,
      status  character varying(10) NOT NULL,
      type  character varying(50),
      msg_id  character varying(255),
      creation_time timestamp without time zone,
      topic character varying(255),
      CONSTRAINT umb_message_pkey_ PRIMARY KEY (id)
    );

    CREATE INDEX idx_umb_message_type ON umb_message (type);
    CREATE INDEX idx_umb_message_consumer ON umb_message (consumer);
    CREATE INDEX idx_umb_message_status ON umb_message (status);

    INSERT INTO db_version(version, creation_time) VALUES ('00013', now());

COMMIT;
