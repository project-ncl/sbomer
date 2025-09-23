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

/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- UMB request event for the pnc build ARYT3LBXDVYAC
INSERT INTO request (id, receival_time, event_type, event_status, request_config, event)
  VALUES ('build_ARYT3LBXDVYAC', '2024-10-14 14:18:45.148407', 'UMB', 'SUCCESS',
  '{
     "type": "pnc-build",
     "apiVersion": "sbomer.jboss.org/v1alpha1",
     "buildId": "ARYT3LBXDVYAC"
   }',
  '{
     "creation_time": "2024-10-14 12:18:45.148407",
     "destination": "topic://VirtualTopic.eng.pnc.builds",
     "consumer": "PNC",
     "msg_status": "ACK",
     "msg_type": "pnc-build",
     "msg_id": "ID:orch-86-qmrdq-44443-1697588407649-5:1:3:1:1",
     "msg": {
        "attribute": "state-change",
        "build": {
           "buildConfigRevision": {
           "buildScript": "mvn clean",
           "buildType": "MVN",
           "creationTime": 1729490967.781,
           "defaultAlignmentParams": "",
           "id": "17068",
           "name": "modelmesh-2.16.0",
           "rev": 2728037,
           "scmRevision": "rhoai-2.16"
        },
        "buildContentId": "build-ARYT3LBXDVYAC",
        "environment": {
           "attributes": {
             "JDK": "17.0",
             "MAVEN": "3.9.1",
             "NODEJS": "18",
             "NPM": "8",
             "OS": "Linux"
           },
           "deprecated": false,
           "description": "OpenJDK 17.0; RHEL 8; Mvn 3.9.1; Nodejs 18; npm 8 [builder-rhel-8-j17-mvn3.9.1-nodejs18-npm8:1.0.7]",
           "hidden": false,
           "id": "807",
           "name": "OpenJDK 17.0; RHEL 8; Mvn 3.9.1; Nodejs 18; npm 8",
           "systemImageId": "builder-rhel-8-j17-mvn3.9.1-nodejs18-npm8:1.0.7"
        },
        "id": "ARYT3LBXDVYAC",
        "progress": "FINISHED",
        "scmRepository": {
           "externalUrl": "https://github.com/red-hat-data-services/modelmesh",
           "id": "1232"
        },
        "startTime": 1730699417.52,
        "status": "SUCCESS",
        "submitTime": 1730699417.193
       },
       "oldStatus": "BUILDING"
     }
   }'
);

-- UMB request event for the pnc operation BDQXCNRZJYYAA
INSERT INTO request (id, receival_time, event_type, event_status, request_config, event)
  VALUES ('operation_BDQXCNRZJYYAA', '2024-10-14 14:18:45.148407', 'UMB', 'IGNORED',
  '{
     "type": "pnc-operation",
     "apiVersion": "sbomer.jboss.org/v1alpha1",
     "operationId": "BDQXCNRZJYYAA"
   }',
  '{
     "creation_time": "2024-10-14 12:18:45.148407",
     "destination": "topic://VirtualTopic.eng.pnc.builds",
     "consumer": "PNC",
     "msg_status": "ACK",
     "msg_type": "pnc-operation",
     "msg_id": "ID:orch-86-qmrdq-33543-1697588407649-5:1:3:1:1",
     "msg": {
        "attribute": "deliverable-analysis-state-change",
        "deliverablesUrls": [
          "https://download.com/rhbk-22.0.5.ER1-quarkus-dist.zip"
         ],
        "milestoneId": "1921",
        "operationId": "BDQXCNRZJYYAA",
        "result": "SUCCESSFUL",
        "status": "FINISHED"
     }
   }'
);

-- UMB request event for the status change of errata advisory 139787
INSERT INTO request (id, receival_time, event_type, event_status, request_config, event)
  VALUES ('errata_139787', '2024-10-14 14:18:45.148407', 'UMB', 'IGNORED',
  '{
     "type": "errata-advisory",
     "apiVersion": "sbomer.jboss.org/v1alpha1",
     "advisoryId": "139787"
   }',
  '{
     "creation_time": "2024-10-14 12:18:45.148407",
     "destination": "topic://VirtualTopic.eng.errata.activity.status",
     "consumer": "ERRATA",
     "msg_status": "ACK",
     "msg_type": "errata-advisory",
     "msg_id": "ID:umb-prod-2.umb-001.prod.us-east-1.aws.redhat.com-44939-1728675652022-7:105726:-1:1:1",
     "msg": {
        "content_types": [
         "rpm"
        ],
        "errata_id": 139787,
        "errata_status": "REL_PREP",
        "from": "QE",
        "fulladvisory": "RHSA-2024:139787-04",
        "product": "RHEL",
        "release": "RHEL-8.8.0.Z.EUS",
        "skip_customer_notifications": false,
        "synopsis": "Important: thunderbird security update",
        "to": "REL_PREP",
        "type": "RHSA",
        "when": "2024-10-28 15:37:17 UTC",
        "who": "xx@redhat.com"
      }
   }'
);

-- Unkown UMB request event from PNC
INSERT INTO request (id, receival_time, event_type, event_status, request_config, event)
  VALUES ('XXXYYYY', '2023-10-14 14:18:45.148407', 'UMB', 'IGNORED',
    null,
  '{
     "creation_time": "2024-10-14 12:18:45.148407",
     "destination": "topic://VirtualTopic.eng.pnc.builds",
     "consumer": "PNC",
     "msg_status": "NACK",
     "msg_type": "unknown",
     "msg_id": "ID:orch-86-qmrdq-33543-16-5:1:3:1:1",
     "msg": {
        "attribute": "unknown-attribute"
     }
   }'
);

-- REST request event for the pnc build ARYT3LBXDVYAC
INSERT INTO request (id, receival_time, event_type, event_status, request_config, event)
  VALUES ('build_ARYT3LBXDVYAC_rest', '2023-10-14 14:18:45.148407', 'REST', 'IGNORED',
  '{
     "type": "pnc-build",
     "apiVersion": "sbomer.jboss.org/v1alpha1",
     "buildId": "ARYT3LBXDVYAC"
   }',
  '{
     "method": "POST",
     "destination": "/api/v1beta1/generations",
     "address": "1.2.3.4",
     "username": "<none>",
     "trace_id": "00000000",
     "span_id": "00000000"
   }'
);

-- Request associated with an UMB event
INSERT INTO sbom_generation_request(
		id,
		creation_time,
		identifier,
                request_id,
		status,
		result,
		reason,
		config,
                type
	)
VALUES (
		'AASSBB',
		now(),
		'ARYT3LBXDVYAC',
		'build_ARYT3LBXDVYAC',
		'FINISHED',
		'SUCCESS',
		'It succeeded',
		'{
"buildId": "ARYT3LBXDVYAC",
"type": "pnc-build",
"products": [
{
"generator": {
"args": "--config-file .domino/manifest/quarkus-bom-config.json --warn-on-missing-scm",
"type": "maven-domino",
"version": "0.0.90"
},
"processors": [
{
"type": "default"
},
{
"type": "redhat-product",
"errata": {
"productName": "RHBQ",
"productVariant": "8Base-RHBQ-2.13",
"productVersion": "RHEL-8-RHBQ-2.13"
}
}
]
}
],
"apiVersion": "sbomer.jboss.org/v1alpha1",
"environment": {
      "maven": "3.9.6",
      "java": "17"
    }
}',
  'BUILD'
	);

INSERT INTO sbom(
		id,
		root_purl,
		creation_time,
		identifier,
		generationRequest_id,
		config_index,
		sbom
	)
VALUES (
		'416640206274228224',
		'pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom',
		now(),
		'ARYT3LBXDVYAC',
		'AASSBB',
		0,
		'{
  "version": 1,
  "metadata": {
    "tools": [
      {
        "name": "CycloneDX Maven plugin",
        "hashes": [
          { "alg": "MD5", "content": "436dfb24c63a322708e4a5143d8543af" },
          {
            "alg": "SHA-1",
            "content": "3ef2e8728dc9eff2f129c34057e46e9d2b10629a"
          },
          {
            "alg": "SHA-256",
            "content": "c185b79bf0f9264f43bc8c222c44db67bea275bb727e3327cf9aea6bf45292c9"
          },
          {
            "alg": "SHA-512",
            "content": "41488733e31e6c5315327dcc86187c2b9a6b96cd08f7e2157ce130688afde29c313fc6a73edb8a76333a930c78874aa2eac874448920030c0f7af0ac05e65ced"
          },
          {
            "alg": "SHA-384",
            "content": "f9462f86240fd9fc0942f34981e6d2371f252aa1a4e5e8c3c14e7f0f1a4e4de69ef972e51cf09a749afb6d97c13e67ed"
          },
          {
            "alg": "SHA3-384",
            "content": "c755af6239348bc7dd1e1e6b5f14f7cf7e1560d689b252d9610cd865fa4b419d0b8e26260731dfc5da2176b6e706b414"
          },
          {
            "alg": "SHA3-256",
            "content": "309aac91005470c9c834354ab918759e2ae7ec52fc1676002621a5bbdf3b7827"
          },
          {
            "alg": "SHA3-512",
            "content": "c634570dcf8302e61005e5fa91a9a0d9b81542f731967b3c9f6d3ef89e3b945bd36d7ab69e2ebad8124b9c05da504361c8b36a8ce528d844409eeb7df4eea6ce"
          }
        ],
        "vendor": "OWASP Foundation",
        "version": "2.7.7"
      }
    ],
    "component": {
      "name": "microprofile-graphql-parent",
      "purl": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
      "type": "library",
      "group": "org.eclipse.microprofile.graphql",
      "bom-ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
      "version": "1.1.0.redhat-00008",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "pedigree": {
        "commits": [
          {
            "uid": "08ad125da45653137814201b4d8527a1abba1e98",
            "url": "https://code.engineering.redhat.com/gerrit/eclipse/microprofile-graphql.git#1.1.0.redhat-00008"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Code-first API for developing GraphQL services in MicroProfile",
      "externalReferences": [
        { "url": "http://microprofile.io", "type": "website" },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/ARYT3LBXDVYAC",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.4",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    "timestamp": "2023-04-21T08:19:06Z",
    "properties": [
      { "name": "maven.goal", "value": "makeAggregateBom" },
      { "name": "maven.scopes", "value": "compile,provided,runtime,system" }
    ]
  },
  "bomFormat": "CycloneDX",
  "components": [
    {
      "name": "microprofile-graphql-spec",
      "purl": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
      "type": "library",
      "group": "org.eclipse.microprofile.graphql",
      "bom-ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
      "version": "1.1.0.redhat-00008",
      "pedigree": {
        "commits": [
          {
            "uid": "08ad125da45653137814201b4d8527a1abba1e98",
            "url": "https://code.engineering.redhat.com/gerrit/eclipse/microprofile-graphql.git#1.1.0.redhat-00008"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "externalReferences": [
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/ARYT3LBXDVYAC",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.4",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql.git",
          "type": "vcs",
          "comment": ""
        }
      ]
    },
    {
      "name": "microprofile-graphql-api",
      "purl": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
      "type": "library",
      "group": "org.eclipse.microprofile.graphql",
      "bom-ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
      "version": "1.1.0.redhat-00008",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "pedigree": {
        "commits": [
          {
            "uid": "08ad125da45653137814201b4d8527a1abba1e98",
            "url": "https://code.engineering.redhat.com/gerrit/eclipse/microprofile-graphql.git#1.1.0.redhat-00008"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Code-first GraphQL APIs for MicroProfile :: API",
      "externalReferences": [
        {
          "url": "http://microprofile.io/microprofile-graphql-api",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql/microprofile-graphql-api",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/ARYT3LBXDVYAC",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.4",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "org.osgi.annotation.versioning",
      "purl": "pkg:maven/org.osgi/org.osgi.annotation.versioning@1.1.0?type=jar",
      "type": "library",
      "group": "org.osgi",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "9e7e55c1937b223e6d85d9376864bdb1" },
        {
          "alg": "SHA-1",
          "content": "f6954fdcee1f910599fcb304522f9168c3e9cd27"
        },
        {
          "alg": "SHA-256",
          "content": "ae98f705c2e624b262c02bcacb8b1f033349e82371ac8d41f2ffc242fde5766f"
        },
        {
          "alg": "SHA-512",
          "content": "680e829c381075de48956fa02f8f82a16548a3bf8216f801ca8598c443d21d55d9c2c72232b6592a24cc059b3e6f1a1ab1678ce476eddea51514b9af15de8748"
        },
        {
          "alg": "SHA-384",
          "content": "02baf45a4841b5f404ea72e0f5e50c992700362983718ba35b305f98390abe0b7275200bb63b93ab5414861646cc4633"
        },
        {
          "alg": "SHA3-384",
          "content": "baf7b64a5542db34ebf11447de52caeaf5d11b2cf79a2b7aeaad90d953eb704b2a0a5bea6da99e4f7067973ac08f87d3"
        },
        {
          "alg": "SHA3-256",
          "content": "b95e87a482896579e2b86ac8d286057aef2f61632271e279a0fda14f6bcf29db"
        },
        {
          "alg": "SHA3-512",
          "content": "19182a56038582758da15363a0af6f7d33dfab3cb75b16601e18d26a97ee1e1f1e4f35dfa323f33d898068c486eafec87f38b622ceba2e304acb4a46c229fa25"
        }
      ],
      "bom-ref": "pkg:maven/org.osgi/org.osgi.annotation.versioning@1.1.0?type=jar",
      "version": "1.1.0",
      "licenses": [
        {
          "license": {
            "id": "Apache-2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0"
          }
        }
      ],
      "publisher": "OSGi Alliance",
      "description": "OSGi Companion Code for org.osgi.annotation.versioning Version 1.1.0",
      "externalReferences": [
        { "url": "https://www.osgi.org/", "type": "website" },
        { "url": "https://osgi.org/gitweb/build.git", "type": "vcs" }
      ]
    },
    {
      "name": "testng",
      "purl": "pkg:maven/org.testng/testng@7.0.0?type=jar",
      "type": "library",
      "group": "org.testng",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "2bba0177e767bd02dbe513297ad62fa9" },
        {
          "alg": "SHA-1",
          "content": "14b73f64988eda81a42b4584e9647d48633ef857"
        },
        {
          "alg": "SHA-256",
          "content": "e7e42b841925f97dc1360fda54b1f8d8ff24be5df1e684105f6167f2bf1a47c7"
        },
        {
          "alg": "SHA-512",
          "content": "dc5a6cc77e1edcead85f2af78253bb38487ee0584394621b1faf5b505d6967fc179d7ee9da660cd1ef120fe0da907522c4744b7b71a55f0bf432f544bfd26e98"
        },
        {
          "alg": "SHA-384",
          "content": "075d367b3f739c1b5e19eb5018e82757531cedccfe2c773183082bc5d102b0230656b290e8c222ea9a004e17816f5cc4"
        },
        {
          "alg": "SHA3-384",
          "content": "c970244c2847ec447aa42bf70bbd8d972ac87ff7229fe5564b2dc288efb5ea879f50a10af39d6838c93a0f9bad1ef6d2"
        },
        {
          "alg": "SHA3-256",
          "content": "66ff783832c3be4f3114e0c76d950ff063c431c7e1a7b3e53eb12150badeda77"
        },
        {
          "alg": "SHA3-512",
          "content": "4b980812cceee6e41183e7a47b839eb30e21d52932c00ac57c45b3772dbefdf26330fbd9ca8ef144f7d76c9071c78c41d22c9d5b1a1af9edd32c65afb0aeda09"
        }
      ],
      "bom-ref": "pkg:maven/org.testng/testng@7.0.0?type=jar",
      "version": "7.0.0",
      "licenses": [
        { "license": { "name": "Apache Version 2.0, January 2004" } }
      ],
      "description": "Testing framework for Java",
      "externalReferences": [
        { "url": "http://github.com/cbeust/testng", "type": "website" },
        { "url": "https://github.com/cbeust/testng.git", "type": "vcs" }
      ]
    },
    {
      "name": "jcommander",
      "purl": "pkg:maven/com.beust/jcommander@1.72?type=jar",
      "type": "library",
      "group": "com.beust",
      "hashes": [
        { "alg": "MD5", "content": "9fde6bc0ba1032eceb7267fd1ad1657b" },
        {
          "alg": "SHA-1",
          "content": "6375e521c1e11d6563d4f25a07ce124ccf8cd171"
        },
        {
          "alg": "SHA-256",
          "content": "e0de160b129b2414087e01fe845609cd55caec6820cfd4d0c90fabcc7bdb8c1e"
        },
        {
          "alg": "SHA-512",
          "content": "2da86589ff7ecbb53cfce845887155bca7400ecf2fdfdef7113ea03926a195a1cbcf0c071271df6bedc5cdfa185c6576f67810f6957cd9b60ab3600a4545420e"
        },
        {
          "alg": "SHA-384",
          "content": "83bb719dacbf29ee808a8d0ed101c67fd0b45e0444ed213373dca56b3a1fa54371cf182e55af7dca7dd38d0e1e5f6cc9"
        },
        {
          "alg": "SHA3-384",
          "content": "604aa0755c858218ca559bd699b3417bac28fb19299082de84ac16dd5773a04191c1c3452aefdf7db572b4e6ec91edc4"
        },
        {
          "alg": "SHA3-256",
          "content": "5a8e259d5be9c8de910280efaa53d1990b4774f3be96fb66640af0d7b2f73468"
        },
        {
          "alg": "SHA3-512",
          "content": "e8be0db4c3222888c3e63d6fa1eefb78b00c3763ac174bdb38a6af045e1c517c78ba9de2afd65206f176150cb2b238c25d2950c7ca995f782c9f9fd0f7fe4ee5"
        }
      ],
      "bom-ref": "pkg:maven/com.beust/jcommander@1.72?type=jar",
      "version": "1.72",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "description": "Command line parsing",
      "externalReferences": [
        { "url": "http://jcommander.org", "type": "website" },
        { "url": "http://github.com/cbeust/jcommander", "type": "vcs" }
      ]
    },
    {
      "name": "microprofile-graphql-tck",
      "purl": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-tck@1.1.0.redhat-00008?type=jar",
      "type": "library",
      "group": "org.eclipse.microprofile.graphql",
      "bom-ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-tck@1.1.0.redhat-00008?type=jar",
      "version": "1.1.0.redhat-00008",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "pedigree": {
        "commits": [
          {
            "uid": "08ad125da45653137814201b4d8527a1abba1e98",
            "url": "https://code.engineering.redhat.com/gerrit/eclipse/microprofile-graphql.git#1.1.0.redhat-00008"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Code-first GraphQL APIs for MicroProfile :: TCK",
      "externalReferences": [
        {
          "url": "http://microprofile.io/microprofile-graphql-tck",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://github.com/eclipse/microprofile-graphql/microprofile-graphql-tck",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/ARYT3LBXDVYAC",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.4",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.enterprise.cdi-api",
      "purl": "pkg:maven/jakarta.enterprise/jakarta.enterprise.cdi-api@2.0.2.redhat-00004?type=jar",
      "type": "library",
      "group": "jakarta.enterprise",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "ac39b41103283099fd5053fe3815e5c0" },
        {
          "alg": "SHA-1",
          "content": "bfbe6ab87b236d9e843d787cdd707842f3a635a7"
        },
        {
          "alg": "SHA-256",
          "content": "e20bdc90144f4e5136472a40f6c7145f4d6ed8be7537d626a9876d01709d0522"
        },
        {
          "alg": "SHA-512",
          "content": "6b1eee1401cb8f7dd613cacb1208af9a8776c9732d055079cfd8911fc5457c8a0861c1e0702acfd732b500f6d07555113c178f6bad9bf052141d7fae857f486f"
        },
        {
          "alg": "SHA-384",
          "content": "c0708138c569d10b7d79d30772d29e0c3e842302970534d9146f526463950eebf34fc70e00e664e0e4e048e12f36b113"
        },
        {
          "alg": "SHA3-384",
          "content": "91c98c8ff51b7b9df74fda101a5f28daf235731a295576dcd9251157f12ccaced70bf38a1640fc181fa0b0ac8d9b474a"
        },
        {
          "alg": "SHA3-256",
          "content": "51628c2afa48c1489ec6755dfaeafb3b004df734b61bb1910336817f573e186c"
        },
        {
          "alg": "SHA3-512",
          "content": "18aba4fd538886075a091b6f36fb8ed53e222211be24037e5f23696278035d4200dc9f818e55ec8464f92c826dada8c54eacce0cfe97e22c2db97e069e6f36e2"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.enterprise/jakarta.enterprise.cdi-api@2.0.2.redhat-00004?type=jar",
      "version": "2.0.2.redhat-00004",
      "licenses": [
        {
          "license": {
            "id": "Apache-2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0"
          }
        }
      ],
      "pedigree": {
        "commits": [
          {
            "uid": "2c1089cff7ffa6eb45e7cbd2dc38df33e58e7b58",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/cdi.git#2.0.2.redhat-00004-2c1089cf"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "APIs for Jakarta CDI (Contexts and Dependency Injection)",
      "externalReferences": [
        { "url": "http://cdi-spec.org", "type": "website" },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/cdi/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/jakarta.ee-community/",
          "type": "mailing-list"
        },
        { "url": "scm:git:git@github.com:cdi-spec/cdi.git", "type": "vcs" },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/AMV6TNUTYBIAA",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.3",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.interceptor-api",
      "purl": "pkg:maven/jakarta.interceptor/jakarta.interceptor-api@1.2.5.redhat-00002?type=jar",
      "type": "library",
      "group": "jakarta.interceptor",
      "hashes": [
        { "alg": "MD5", "content": "04eeeb7a4477e5adfe4d677e251cc453" },
        {
          "alg": "SHA-1",
          "content": "e82101a4397134c2644c70efb92ab0d270f050f1"
        },
        {
          "alg": "SHA-256",
          "content": "775de59d99d1f5c7361910cf3c58a260c33756ba72adc6615c0a5578657295cd"
        },
        {
          "alg": "SHA-512",
          "content": "1b57134df8a48a4196a353cdaa4923693188f09c32d0e042360f049554270e29f7332075b3e5c8d26349f5966a57714fbce74393a9d376e04e1281ed77730a3e"
        },
        {
          "alg": "SHA-384",
          "content": "4a62c46c5a60ca3545630863ff2895fde88999e330291ecf4c0d527e5b332d8a98117369568c5797492bfe0905d11238"
        },
        {
          "alg": "SHA3-384",
          "content": "9b853eb617242b81d11c3308cefc6925978a63e236627a13181ae0a70d0116db5f5b35d1695a47aba925e13284b41d56"
        },
        {
          "alg": "SHA3-256",
          "content": "4576cb450f54ac0bfcbbd50ea99c31cc0329d729538837a8997fa9826eb97827"
        },
        {
          "alg": "SHA3-512",
          "content": "8013d203120eb7568176e13a4906e3a40b1958b390d6771c8fb3e93bb81bd0f63c26aa6f55b778441b40980bf1672c7f0c59368783e244eaf42d7defea6ec3cc"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.interceptor/jakarta.interceptor-api@1.2.5.redhat-00002?type=jar",
      "version": "1.2.5.redhat-00002",
      "licenses": [
        { "license": { "id": "EPL-2.0" } },
        { "license": { "id": "GPL-2.0-with-classpath-exception" } }
      ],
      "pedigree": {
        "commits": [
          {
            "uid": "044106f8b863c0be000fc46798bebda8f8b858c3",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/interceptor-api.git#1.2.5.redhat-00002"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Jakarta Interceptors defines a means of interposing on business method invocations and specific events?such as lifecycle events and timeout events?that occur on instances of Jakarta EE components and other managed classes.",
      "externalReferences": [
        {
          "url": "https://github.com/eclipse-ee4j/interceptor-api",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/interceptor-api/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/jakarta.ee-community/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/eclipse-ee4j/interceptor-api",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/45378",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.annotation-api",
      "purl": "pkg:maven/jakarta.annotation/jakarta.annotation-api@1.3.5.redhat-00002?type=jar",
      "type": "library",
      "group": "jakarta.annotation",
      "hashes": [
        { "alg": "MD5", "content": "2a23564e430800fb8fb26e033e5cf03a" },
        {
          "alg": "SHA-1",
          "content": "4cf6d37350212aa4f0501de8667154b61c3a5084"
        },
        {
          "alg": "SHA-256",
          "content": "a4bf0992254b1cb791eb48b8d8b534b7cc840fd7b4d06213426027b709014370"
        },
        {
          "alg": "SHA-512",
          "content": "1558e9165515d96cb6fd67311c3767358918d310d9380be4c0ca76e438d48025882d7af7d2fe72a5c9aea65ff345dde98450e34df6ce58d798bd9d5750595f8c"
        },
        {
          "alg": "SHA-384",
          "content": "44ea649d5649594308df48ee9c8dc59c8abd9485070832d6fde17c7671432f39e9f566e06d32d7d96012abc25fbcd9b1"
        },
        {
          "alg": "SHA3-384",
          "content": "bd78ad3f0451784370c745a5747c18c3ddb77d3ea4342ae46d272020e69fcf89e3c2dc26e43d1cb90221b63bbb4e6bb2"
        },
        {
          "alg": "SHA3-256",
          "content": "d83659fc2f90f6be90e706f9908e93e5f013c355bdd8ed6fe7f2c82b5b6c94b3"
        },
        {
          "alg": "SHA3-512",
          "content": "41b77d310d882472ed01c692f80aa414f1c1995a4827c73fdd140f690c64399d9617cf823de901957bb195c016778132a2d820a66725934d89031ecf8674d125"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.annotation/jakarta.annotation-api@1.3.5.redhat-00002?type=jar",
      "version": "1.3.5.redhat-00002",
      "licenses": [
        { "license": { "id": "EPL-2.0" } },
        { "license": { "id": "GPL-2.0-with-classpath-exception" } }
      ],
      "pedigree": {
        "commits": [
          {
            "uid": "28c16af8b619a9781a6b71633025a4520485ecfb",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/common-annotations-api.git#1.3.5.redhat-00002"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Jakarta Annotations API",
      "externalReferences": [
        {
          "url": "https://projects.eclipse.org/projects/ee4j.ca",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/common-annotations-api/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/ca-dev",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/eclipse-ee4j/common-annotations-api",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/45368",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.6.3:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.ejb-api",
      "purl": "pkg:maven/jakarta.ejb/jakarta.ejb-api@3.2.6?type=jar",
      "type": "library",
      "group": "jakarta.ejb",
      "hashes": [
        { "alg": "MD5", "content": "476df38dce227910570d7d1104f49089" },
        {
          "alg": "SHA-1",
          "content": "3c52a831bfe8118a27ed393234f1d1d3baa50a7c"
        },
        {
          "alg": "SHA-256",
          "content": "47bf067a03e2f4d57e37ff8acacfa57caefbf89d77167dc6f5c36896aaf8b1c3"
        },
        {
          "alg": "SHA-512",
          "content": "086c2ce8f9da22146d6fa1d96289700cfe11c6bf37542309a06a4779b8749657c7a477431a715430c0af2a66d85a188854a707c32d7772a09a838e2824b70a8f"
        },
        {
          "alg": "SHA-384",
          "content": "e0e638b0e590c5ecdeb259c2fb2825e0bfa6b5137c3fc821a713a16d3d715588a28a4b140a7304158381cfb5f965cf6a"
        },
        {
          "alg": "SHA3-384",
          "content": "c4e905907660751a873972f743c434d3f3017114908a45e28a91d3a6946262490e2cc7847501b06feff7c3982185dfbe"
        },
        {
          "alg": "SHA3-256",
          "content": "1968781ac2bf53e09e4781be0ff0831bd4a0e72a3f268e3f7f7e855a23001aa1"
        },
        {
          "alg": "SHA3-512",
          "content": "4c43b2298d53db0e8f0aa80a2b9db783df1efb37a6157d098068966fd44e95d8e0eb27ddb26ff84b81809bb423a1e3bfe394b153938bca3b1b7274385ed22771"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.ejb/jakarta.ejb-api@3.2.6?type=jar",
      "version": "3.2.6",
      "licenses": [
        { "license": { "id": "EPL-2.0" } },
        { "license": { "id": "GPL-2.0-with-classpath-exception" } }
      ],
      "publisher": "Eclipse Foundation",
      "description": "Jakarta Enterprise Beans",
      "externalReferences": [
        {
          "url": "https://github.com/eclipse-ee4j/ejb-api",
          "type": "website"
        },
        {
          "url": "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/ejb-api/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/jakarta.ee-community/",
          "type": "mailing-list"
        },
        { "url": "https://github.com/eclipse-ee4j/ejb-api", "type": "vcs" }
      ]
    },
    {
      "name": "jakarta.transaction-api",
      "purl": "pkg:maven/jakarta.transaction/jakarta.transaction-api@1.3.2?type=jar",
      "type": "library",
      "group": "jakarta.transaction",
      "hashes": [
        { "alg": "MD5", "content": "af630b52e4ea40ea6f9b6f38734d20fa" },
        {
          "alg": "SHA-1",
          "content": "76be6fa74d94c97841d7804b9870f85a20a3da32"
        },
        {
          "alg": "SHA-256",
          "content": "0dda6ab4160077a5f18d5b24994a44e24576058db9b3da65dfbf14e9e750ce2c"
        },
        {
          "alg": "SHA-512",
          "content": "02d8b4d559073298a2c49e41bc4dddd902c5e4142a56439188d6212cf5386c966a02aed2825993a25429ca5b7291eb3b60e57e1868f0a0197390fc40c37f4c4b"
        },
        {
          "alg": "SHA-384",
          "content": "f4045cc20f809ae1a1838aa5e792b1a44e44a32f4121bce02ebd566bc4a445288c0b2f7afe9dbc1e47259f6d10e78171"
        },
        {
          "alg": "SHA3-384",
          "content": "4adec1517ca6ebbfa4f80f76b01b71c79f8896d39b88041acfc42bc91a9731ff07f5ef5cd93fdc4624d2cf9eb71cec1f"
        },
        {
          "alg": "SHA3-256",
          "content": "e0508c8836aab7225dfd574039b0d5540ba5b037ef66c71b9a9dd9174287759e"
        },
        {
          "alg": "SHA3-512",
          "content": "f21999137fd964e15194e995ff34b4158a056c52e645f71debc628c8938cfeaadd439e3be883ae4cb9fe595b0760132be679a50ed348ff8fee11f6584ce58a30"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.transaction/jakarta.transaction-api@1.3.2?type=jar",
      "version": "1.3.2",
      "licenses": [
        { "license": { "id": "EPL-2.0" } },
        { "license": { "id": "GPL-2.0-with-classpath-exception" } }
      ],
      "publisher": "EE4J Community",
      "description": "Eclipse Project for JTA",
      "externalReferences": [
        {
          "url": "https://projects.eclipse.org/projects/ee4j.jta",
          "type": "website"
        },
        {
          "url": "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/jta-api/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/jta-dev/",
          "type": "mailing-list"
        },
        { "url": "https://github.com/eclipse-ee4j/jta-api", "type": "vcs" }
      ]
    },
    {
      "name": "jakarta.inject-api",
      "purl": "pkg:maven/jakarta.inject/jakarta.inject-api@1.0.0.redhat-00002?type=jar",
      "type": "library",
      "group": "jakarta.inject",
      "hashes": [
        { "alg": "MD5", "content": "85cf6916f2487e6151ac235aaa6b2219" },
        {
          "alg": "SHA-1",
          "content": "6255ecd2bfa30496d39bfdc7c7b1c5d75d541ad7"
        },
        {
          "alg": "SHA-256",
          "content": "eeb057dd7c98050e8bfb7c008c2320649fcab724d4c23dfbb0a165913a8ddc96"
        },
        {
          "alg": "SHA-512",
          "content": "b49dc388d9ec9065c17e31cf7544f46b9528111980dc4fc5d543ce25a68ec09299c9d2b4ea32e85a24b19d691139a8b703e7f929d37b24c032f059ed052b01bd"
        },
        {
          "alg": "SHA-384",
          "content": "40ef2302872e1b5a0dec393ef6f1437d5963d3408dec4d7255669f7d515b600a464ae9888885ebc856d24cf1eabfe7b1"
        },
        {
          "alg": "SHA3-384",
          "content": "758d9c68da78f70b081a9cf50128f8f8647e55052bca5ecc2e1bfd50262523d7534dc7c12d2f94d7ee6341a049d495b2"
        },
        {
          "alg": "SHA3-256",
          "content": "75cc2599d47ce64c3a40687cfd11bd3ec27b286b7ae58d5a1e07856095dbf7a1"
        },
        {
          "alg": "SHA3-512",
          "content": "baea24e75b04e60ab92e0d2d93e5a68415299b8bb5c620fb06924ed75611bf028d326d19bb6060f6343eef69af0297430e8cb7cc6300d8628d4d59744d074983"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.inject/jakarta.inject-api@1.0.0.redhat-00002?type=jar",
      "version": "1.0.0.redhat-00002",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "pedigree": {
        "commits": [
          {
            "uid": "9b228c657b5d86511dae6299469110f52b6ecd2f",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/injection-api.git#1.0.0.redhat-00002"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Jakarta Dependency Injection",
      "externalReferences": [
        {
          "url": "https://github.com/eclipse-ee4j/injection-api",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        { "url": "https://issues.jboss.org/", "type": "issue-tracker" },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/eclipse-ee4j/injection-api",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/33383",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.3.9:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.json.bind-api",
      "purl": "pkg:maven/jakarta.json.bind/jakarta.json.bind-api@1.0.2.redhat-00003?type=jar",
      "type": "library",
      "group": "jakarta.json.bind",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "3bb3730516508b2e6eadec1263ab3a36" },
        {
          "alg": "SHA-1",
          "content": "26c8565a988fedb49a2154ba5c0bd26d576e98a4"
        },
        {
          "alg": "SHA-256",
          "content": "2a85bbcfe1dddf477600a1f2b3d8161f33d4791c046da479631f01ffbe46f3c8"
        },
        {
          "alg": "SHA-512",
          "content": "567f1fa771796ae9c337c16a12642750d16c04ddfcaa0ed9d0567ba789781a96701312500d5107fa9a09ac9e61fd74a0c7be8b883f59f35624369790b697d639"
        },
        {
          "alg": "SHA-384",
          "content": "0c1deacdf2a9dc2567d0f46229afcc83909b9ebd8d1ca5e49fa608a7dceb5c07799131e073f7ce5b1f224461b4f446cb"
        },
        {
          "alg": "SHA3-384",
          "content": "2dbff0873d3f6d8c81d872173ea3820dad36555347b01652707fd748e439f66c6bac8b5cf4cebce5260ecd8eed53f82a"
        },
        {
          "alg": "SHA3-256",
          "content": "e24581016fc8fb8b6be733d68fc06e8a138bb0083a80e915ef842ef9434573d0"
        },
        {
          "alg": "SHA3-512",
          "content": "0e73be9b1e38fd40669063ce59ab353fe0bd9e508c2c6bea0f297970b99bcb03321200533b8b6926cbeca6955de3aeb7befcf772c766cc23bedde4d671baa27a"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.json.bind/jakarta.json.bind-api@1.0.2.redhat-00003?type=jar",
      "version": "1.0.2.redhat-00003",
      "licenses": [
        {
          "license": {
            "id": "EPL-2.0",
            "url": "https://www.eclipse.org/legal/epl-2.0"
          }
        },
        {
          "license": {
            "url": "https://projects.eclipse.org/license/secondary-gpl-2.0-cp",
            "name": "GNU General Public License, version 2 with the GNU Classpath Exception"
          }
        }
      ],
      "pedigree": {
        "commits": [
          {
            "uid": "95734ec5943a408512a1227bfe55f0254267a0bd",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/jsonb-api.git#1.0.2.redhat-00003"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Jakarta JSON Binding is a standard binding layer for converting Java objects to/from JSON documents.",
      "externalReferences": [
        {
          "url": "https://eclipse-ee4j.github.io/jsonb-api",
          "type": "website"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/jsonb-api/issues",
          "type": "issue-tracker"
        },
        { "url": "jsonb-dev@eclipse.org", "type": "mailing-list" },
        { "url": "https://github.com/eclipse-ee4j/jsonb-api", "type": "vcs" },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/60264",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j11-mvn3.6.0:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jakarta.json-api",
      "purl": "pkg:maven/jakarta.json/jakarta.json-api@1.1.6.redhat-00003?type=jar",
      "type": "library",
      "group": "jakarta.json",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "b28574608df15321d60a2fd5857033a3" },
        {
          "alg": "SHA-1",
          "content": "fd08e962a56c6179eba206ca306d455c40568ffd"
        },
        {
          "alg": "SHA-256",
          "content": "ac2501634f129735a7227966426134004897641e5a8a8b4520845324013abcd7"
        },
        {
          "alg": "SHA-512",
          "content": "aab603469c8ca365b0716246f2ddf6ec1628c47890e0cf07095c37c3dc1024aa276fa0bf7194d7b61f0b384b427e17c5276d5c5057bbd1ecbb018d1208587ec4"
        },
        {
          "alg": "SHA-384",
          "content": "1aa93cb25bf84997a3ad189a0e0995a801a3090e8140c6a8ad20c6d40e489f97dac6d9c64191f01100899dcc004bc4c6"
        },
        {
          "alg": "SHA3-384",
          "content": "46e3b7bec4ec4e08114b5665375a0ddf2d108b4ebe073b9784eba622e7a403250aa5e724740926aba146914c4c41acd2"
        },
        {
          "alg": "SHA3-256",
          "content": "b90af9a3791f2c5ddd7c7624a43f1afac3f357c6a2da5e738c387368d7fa3ea3"
        },
        {
          "alg": "SHA3-512",
          "content": "7f1598720e6df58282a706a4a7a8cd22327c9192674401ccb924ec24543adf51ed5a57072a493df524795cf71fe053381c62ba25dada0dcbb0cb6d07673040d5"
        }
      ],
      "bom-ref": "pkg:maven/jakarta.json/jakarta.json-api@1.1.6.redhat-00003?type=jar",
      "version": "1.1.6.redhat-00003",
      "licenses": [
        {
          "license": {
            "id": "EPL-2.0",
            "url": "https://www.eclipse.org/legal/epl-2.0"
          }
        },
        {
          "license": {
            "url": "https://projects.eclipse.org/license/secondary-gpl-2.0-cp",
            "name": "GNU General Public License, version 2 with the GNU Classpath Exception"
          }
        }
      ],
      "pedigree": {
        "commits": [
          {
            "uid": "83609db577c435fb326d3570460561fd15e5e296",
            "url": "http://code.engineering.redhat.com/gerrit/eclipse-ee4j/jsonp.git#1.1.6.redhat-00003"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "Jakarta JSON Processing defines a Java(R) based framework for parsing, generating, transforming, and querying JSON documents.",
      "externalReferences": [
        { "url": "https://github.com/eclipse-ee4j/jsonp", "type": "website" },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "https://github.com/eclipse-ee4j/ee4j/issues",
          "type": "issue-tracker"
        },
        {
          "url": "https://dev.eclipse.org/mhonarc/lists/jakarta.ee-community/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/eclipse-ee4j/jsonp/jakarta.json-api",
          "type": "vcs"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/60274",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j11-mvn3.6.0:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "arquillian-testng-container",
      "purl": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-container@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.testng",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "7b48ec03b5bfe6f8e186857cd0deeaa5" },
        {
          "alg": "SHA-1",
          "content": "e94455df7e14576982aa76a0fc88f7f8bfdc39b7"
        },
        {
          "alg": "SHA-256",
          "content": "b36e5cd715f5bd7814f2368def23f18712c490dc4a5e890bf6cc0bb2cd8cdc8f"
        },
        {
          "alg": "SHA-512",
          "content": "d166742d86e0bad1c17d382e6c31dc437f7e48fc85a5f885c30ae2ba7d40c02a64f956fa45c8dc72fb97ffc4967cf2cfb5d755b0fb990d1ae858dfabbfd32be5"
        },
        {
          "alg": "SHA-384",
          "content": "9812fbe9841112e3bc0db5771250d353e8491abe1959cee55292590bcf7f01efeddabe8d917f02f7b201352ea626cbc6"
        },
        {
          "alg": "SHA3-384",
          "content": "56c3ac04e58bf3e562fdb29582f111e7c0a5eac2e913071ed46e144ec6c638a1f40df4a8071105cf6a21e9ff197a0dd8"
        },
        {
          "alg": "SHA3-256",
          "content": "c959d19f57fd67f8bc4ac33219210f7dc295d1c6d85804b8563cb7e29fd939e9"
        },
        {
          "alg": "SHA3-512",
          "content": "ef16d6e65d17941284215bf1ec68f8363e1a150ac14cf813803d2bda2afc0d44f939f533b98ebfa06d0fab5a440288eeed5690584514b72e66b8e9e4e733d736"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-container@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "TestNG Container Implementation for the Arquillian Project",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-testng-container",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-testng-container",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-testng-core",
      "purl": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-core@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.testng",
      "hashes": [
        { "alg": "MD5", "content": "8bf1aaa261f7f023c88ed2148ad1fd98" },
        {
          "alg": "SHA-1",
          "content": "38f28df9b451c95ee23b8be727021306b4a45d13"
        },
        {
          "alg": "SHA-256",
          "content": "f0c78d3dd8a8fe044c3a0e77addb6b1b004ef47f92ab0d5e0c0e61e39295de90"
        },
        {
          "alg": "SHA-512",
          "content": "2cb761db63880b13eed9ecbb5a25cde9be801e4205e76e2a3d71ee348c41be8d2f5348598ecd885ba3c43227422581c3b574b09c4f9cd3207245e7c18894105d"
        },
        {
          "alg": "SHA-384",
          "content": "6f61cb632ccd6358d3917f73418e02e0fce847b2d2664dcacdd0fb4438bfc5652de12a6a736f14310ef6e8bd50079157"
        },
        {
          "alg": "SHA3-384",
          "content": "3e0ecd981f613d1c9b6334589eee9bdb66e8d9b7a03b833053f6d1271b54dc3eded5cf312b16a05ab3406378a5537ddf"
        },
        {
          "alg": "SHA3-256",
          "content": "c3954fe96abd62ff7846b6e43702797654ed18741ece1e2b651c4428459b0f32"
        },
        {
          "alg": "SHA3-512",
          "content": "8af04e5e000ab3e443fc5168bf7f1fbb47b5310eba38746f89cf0946a0e3daa19a6b597e13e2c8c0f97ca74937d080ec82de056061b1df9a231fb1ce8a527ad6"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-core@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "TestNG Implementations for the Arquillian Project",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-testng-core",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-testng-core",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-test-api",
      "purl": "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.test",
      "hashes": [
        { "alg": "MD5", "content": "6ae797f4a69b4db84d7f9527bd0c51a4" },
        {
          "alg": "SHA-1",
          "content": "d0903e71e88769712f04a52a680bb5a93e760ea8"
        },
        {
          "alg": "SHA-256",
          "content": "3526555599f64c335c937d93656283d4cbd45d7a190e8c710ca6eb6e1aaa73ad"
        },
        {
          "alg": "SHA-512",
          "content": "c228d2e31997cfea424682f10e9b3589c086681ffcbd00ea98d3cc921341fff56026212320082803e6e3e7c1ae4daed0d07b20149c8c77250df9610db6997a99"
        },
        {
          "alg": "SHA-384",
          "content": "b3351b4830542b15a72bb02f0330724b08c494e6f52e5775843ede3576aeee30ab7e5ad0dd58e72f437a160fe2367d45"
        },
        {
          "alg": "SHA3-384",
          "content": "265390e9981a1aee51abc4c3333f50ac6c2d7c0da8a38e90efef2b1132a3fa3332d919062b224bf720b20fb8b3549fdc"
        },
        {
          "alg": "SHA3-256",
          "content": "0026e9b8b79a805acdce447d79f409b8a73ad2611ca2530d4e5939daaa310aae"
        },
        {
          "alg": "SHA3-512",
          "content": "39db1ad686f3e84cc98b4b072a9f5eeb8143dd425523d0670007ac95b28f2c7add0acf4f400c26c8a9ac52c11c4a81af2d16a84c9073ff05e26dc6858dcc88be"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "API for the Test integration.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-test-api",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-test-api",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-core-api",
      "purl": "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.core",
      "hashes": [
        { "alg": "MD5", "content": "9a3f2aa27b9d2fb2005bde97dd8ae0a8" },
        {
          "alg": "SHA-1",
          "content": "49093902333cb18b457f4f9b8870924ef97cefe9"
        },
        {
          "alg": "SHA-256",
          "content": "8faf86ad37a85831336fa0915ef27b982bf2a17069ae25d60a49f3e8fdedbdd9"
        },
        {
          "alg": "SHA-512",
          "content": "d6cf9ddebd1585e6f16f604114d2096571a447ef40cce6026ac94852d4268152dab336fe10662bef55b902f5d7eb70cb477830d12ee7db0ff0cb224e6ace8206"
        },
        {
          "alg": "SHA-384",
          "content": "591b68609bf9d2525c0892eab8e0e2d7c01cc01a46446d755926186459d093f391e445914655750e995375f43cc5a601"
        },
        {
          "alg": "SHA3-384",
          "content": "d7c255ef13f43d49b4f8768a4c49fb3b979983430b11247cd239dbf0020e8c14f49ea0348023a1242786ac6cc633a870"
        },
        {
          "alg": "SHA3-256",
          "content": "8890ea49cff727a52f87a7ec0c4e2d167185b6683f4f73758db10a68eeeeff09"
        },
        {
          "alg": "SHA3-512",
          "content": "cfba493915821848bdce886dfcf2a14620b30736c7bb48f2288ee908c99ed92f4db91c15d2c1c99b58150758e10e96d0a0a149d0ab6662eac40fc0b57df6f1b4"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "API for the Core.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-core-api",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-core-api",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-test-spi",
      "purl": "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.test",
      "hashes": [
        { "alg": "MD5", "content": "3f7d617fbe14e713bc681cc634a6c5bb" },
        {
          "alg": "SHA-1",
          "content": "4208c2f941125b68f2477855be9ebdfcbd62ddee"
        },
        {
          "alg": "SHA-256",
          "content": "dcc70287232c12dede082de87901fc2b397db6b7104250670da1d88a10afa0f9"
        },
        {
          "alg": "SHA-512",
          "content": "7afc7cb8c0d086b07d294b246697b1909551994140cd866b6be2ec1d6ed852524054381ce4d7848b1cda0f2fac6e2fbd0a0f86caa7e5973f84120ba45c3cf41e"
        },
        {
          "alg": "SHA-384",
          "content": "14b28062c0d3849ec6de145d318d9206d485bcd96439bd1e0bb3d92c48491445de1655aa95bbeccc3f9955b49eba4be7"
        },
        {
          "alg": "SHA3-384",
          "content": "c79840737387f19afbd2116eff0dfaa136bb58040e2db1e03772f7602ad90161a826b2951c924fa94a655792bf2a422d"
        },
        {
          "alg": "SHA3-256",
          "content": "c765d8baba62c17759df23c60d1272494110a429d4290039f329ff9ef6f2e91e"
        },
        {
          "alg": "SHA3-512",
          "content": "618b6bbe8989dfcb12f0475c9521a697d65dd4641d97815dbfea14d43008448f8550e3c39e75a097bfc66db97039ed6bdb3185d2570270ec68ca8c2a8be4434b"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "SPI for the Test integration.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-test-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-test-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-core-spi",
      "purl": "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.core",
      "hashes": [
        { "alg": "MD5", "content": "05bcd97aef13c42ae8452b19e257a931" },
        {
          "alg": "SHA-1",
          "content": "66e18c1f136418ccc03bc4618906aa8deb8d9a00"
        },
        {
          "alg": "SHA-256",
          "content": "71ba55a2a46c74cc8aee801ec70b186baf930e3a8c24116b92e072ad8232c6e6"
        },
        {
          "alg": "SHA-512",
          "content": "b2eb15357c557bd44c5dbfffc706f1880aa01a90fdff40b86d3bb2f7da313641c6dd2020a525ff146724fd964b238a994c55c92c8a039d09c797006039658f9d"
        },
        {
          "alg": "SHA-384",
          "content": "ff787d3b895a94e070c47c5c29c6762e5444daaf54bfdbdf321562acbd1531de5895a1657bdc75430453f76b15e79b59"
        },
        {
          "alg": "SHA3-384",
          "content": "93b68eb67b8d0a7b998d67354b088534b2d0c47761a1b0ead8052815cb92e502b8ef71a14ebd6e0efaac8dfb526f7570"
        },
        {
          "alg": "SHA3-256",
          "content": "e79545940f9934332e13d19f1ca5fd583f27e115a849c6b0e99ac15a794a4bf0"
        },
        {
          "alg": "SHA3-512",
          "content": "6ce2aa5c5f31a668a173d4026a08be7d0659c209ae12d3845522e0331fd76d86f9d7b9d557e9c00b01aa1b41e1e344d21127c003b79ab38dcb4c3fdde189a16d"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "SPI for the Core.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-core-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-core-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-container-test-api",
      "purl": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.container",
      "hashes": [
        { "alg": "MD5", "content": "ebab6ecb3bcb5d6ce93f53434f6d45ea" },
        {
          "alg": "SHA-1",
          "content": "38bf1405dd34baf038667044cf9b9020dbd2d3b8"
        },
        {
          "alg": "SHA-256",
          "content": "bc9db4736395ee8913d04f8e02967a08f8b2f28ce0b78b98d12c52bde2690009"
        },
        {
          "alg": "SHA-512",
          "content": "55abf3a3cb7c92ecedfff55992a7d7e1a4c1c5f04ffbf8633f854f69a81fd57260f17651a65747b75ce7887f4d2420dd2bf842ac653873c63d065cc2d3e8f5fe"
        },
        {
          "alg": "SHA-384",
          "content": "91455d2b9aabf7f15fa24ef27fd020ac7d41fa4570d353084b47b31a8a28779003c4a259271164935b6c989e41c9e652"
        },
        {
          "alg": "SHA3-384",
          "content": "616b4ac5278a6728e1816b2ad79192aff0cf603df05910c3f87e2b91d615507c55cce0064b0ecc358365833e904bd8a9"
        },
        {
          "alg": "SHA3-256",
          "content": "9010995938507722fa61c7332b71c0d6ccb1a859292cce6a03cd904135e0ffba"
        },
        {
          "alg": "SHA3-512",
          "content": "2f712c4f1f140fa8f2611c665c44586f288ad87af8d8758f9cd16017f27f03b50727a5ae682838e125db5201a9c1bb042588a9567b531de5f07eeca4af4677e0"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Integration with the Test extention for the container extension.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-container-test-api",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-container-test-api",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "shrinkwrap-descriptors-api-base",
      "purl": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar",
      "type": "library",
      "group": "org.jboss.shrinkwrap.descriptors",
      "hashes": [
        { "alg": "MD5", "content": "40cce2158467992c14cac68817970396" },
        {
          "alg": "SHA-1",
          "content": "50041d636fe33d59d8d5ed06b6660883481c1cff"
        },
        {
          "alg": "SHA-256",
          "content": "9df16f7da71188ccd1646c5d7e0f757ead35394ac6192d05cf1b22e4d04b901b"
        },
        {
          "alg": "SHA-512",
          "content": "39b744dc79f756adeab5f72ceedf6bd7f8efe707cf2fd39f888fe6d42915e08c12727792856fc6e6247637380d5ee4f32ebf95983a3ab12c6974a9c97cffbb17"
        },
        {
          "alg": "SHA-384",
          "content": "2a84f6cc0e1f75d7422f78a826cbe83d6d33e17cc337ea3f97b567fa4a3f9f51028cb5ba2757c0a1d5d8dda66a94bc5c"
        },
        {
          "alg": "SHA3-384",
          "content": "f82225ac639363e32f55958826041f5c0948deabb0aac3f82caf71968ee7e1c4eb7c1a98ee7de50bd635dffe9b87a26c"
        },
        {
          "alg": "SHA3-256",
          "content": "c54b8bcffc407aaabb7d9684c74baabde4a28e434a798759e34d728e8d52bc6a"
        },
        {
          "alg": "SHA3-512",
          "content": "477fb294ff2fb49790cc380b17d2dd381a53ddd681efe41c215182dfb9c8866e55493ae9815e57866070d7091802d58cd7292ee6ece80878a352ebd3c701e802"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar",
      "version": "2.0.0",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Base for Client View of the ShrinkWrap Descriptors Project",
      "externalReferences": [
        {
          "url": "http://www.jboss.org/shrinkwrap-descriptors-api-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/SHRINKDESC",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "http://github.com/shrinkwrap/descriptors/shrinkwrap-descriptors-api-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-core-impl-base",
      "purl": "pkg:maven/org.jboss.arquillian.core/arquillian-core-impl-base@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.core",
      "hashes": [
        { "alg": "MD5", "content": "9d7b57bef99df8bd438b8fbbb432c426" },
        {
          "alg": "SHA-1",
          "content": "125d8c983c57a9b8e5cdde92b2884a2bc980a66d"
        },
        {
          "alg": "SHA-256",
          "content": "973248341f74035fead29ccdbd3f9a96cc3ce8ab4eb7f61b0fcb4e79f78b6847"
        },
        {
          "alg": "SHA-512",
          "content": "4c196077f2153e48245505897535e4d459aad118ba9a50b1a37e55d3b60ec007d21d78acfd48c9b3a8e59a9bda76149c736203e9df4f1666a6fe8d30107e9ad6"
        },
        {
          "alg": "SHA-384",
          "content": "46b01b713e4e8b812e487f8892d8a08666cfad5ebc05f72513b53a360c8dce56ad8a621c8b8796de8504f7a2b7826556"
        },
        {
          "alg": "SHA3-384",
          "content": "180124d20a7044a3ffb972a9cc4b9ac2c9ce2738d18af3af2aa7048f2b0d31a0158e676d7ce9c5e16cb38e56a7046882"
        },
        {
          "alg": "SHA3-256",
          "content": "f88683c07e459a08f8896d3161e285a6ec16d3c12589ecdb6def3e7480944d39"
        },
        {
          "alg": "SHA3-512",
          "content": "e167902c100e08fbb785ea27f57d8d3d2fdfd88b22befe0bfb8ba8d97551a50bc85560740b2442084c387dd64a54c2566789dc219190905526d8bce26ace052d"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-impl-base@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Impl Base for the Core.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-core-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-core-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-test-impl-base",
      "purl": "pkg:maven/org.jboss.arquillian.test/arquillian-test-impl-base@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.test",
      "hashes": [
        { "alg": "MD5", "content": "80545a84606901f955fa34e4880fafbf" },
        {
          "alg": "SHA-1",
          "content": "1ea0a02e645475611a51e600a732059776ddfbae"
        },
        {
          "alg": "SHA-256",
          "content": "3a410bfc87030bd7abafbfa755e946af0d0af7623c56cb513e0bffee4ac1d8dc"
        },
        {
          "alg": "SHA-512",
          "content": "351fb73c6099aa8da104fb29ae50400a7b85d6313e0e72389334a5ae3cf7b04c41fc5f8eae9918860805bcbc30e99430aa749872bf3cf0ba220420d38390cf19"
        },
        {
          "alg": "SHA-384",
          "content": "a8c1d4a401aa2f001b2c9fde1569c0f269f1a8a5581e868529321f885fa9ca7121cc4e3ed5987512feb8b035d982597f"
        },
        {
          "alg": "SHA3-384",
          "content": "740a0225e02cf9b16d0c54b6504b35a83a1d450349b605fc512f371c19972a9c54aa834d0b907deaa93af2f4dc72eac0"
        },
        {
          "alg": "SHA3-256",
          "content": "43903ec0ab3852bf0c7a78573b6d379e5f388b7c0a6834abc233a06557c42499"
        },
        {
          "alg": "SHA3-512",
          "content": "e75bbe98c72a23dff545af2a886f907ca0f489237bb27da75415c0d2bdc217cd1a91235899137a1c57bb74ebe0bfe74531c04be5182ee792726a0c649c661305"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-impl-base@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Implementation Base for the Test integration.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-test-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-test-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-container-impl-base",
      "purl": "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.container",
      "hashes": [
        { "alg": "MD5", "content": "1e921cb9ddc22b35b0853c80884083dd" },
        {
          "alg": "SHA-1",
          "content": "b6bb0988e98b7899e831df94458347769d9bedc7"
        },
        {
          "alg": "SHA-256",
          "content": "f61fbde2ef72bce1e2ff65a3b54ef434b5eefb3575bc475027ddbb4174ba3120"
        },
        {
          "alg": "SHA-512",
          "content": "bf114acd8aca0ed26cebea31cc3c6558e5a1b50b0e392626ac3275e58ff11b0777791db918f12e43a6f7dc6e5ddb32457bc26ce9ce236026dabf8d11a26fa2de"
        },
        {
          "alg": "SHA-384",
          "content": "3b32b6c3c345845ba1d8fa5527eff29044bc9c8627cc187cef7a2e286a7088fe0b68686cd83a4d61b6137238e54299e2"
        },
        {
          "alg": "SHA3-384",
          "content": "6618a0f81c9ac00e8e6334a97deb623fff89864f546b1959726968b77d6f075eae8b07048a096e028812cbcdb38b3fd8"
        },
        {
          "alg": "SHA3-256",
          "content": "92e91d1a7c86828190191be4559b2d34516a7b9350349d80e04303a35a789cc2"
        },
        {
          "alg": "SHA3-512",
          "content": "45430ae4c91ba38df02d8581a8e050d903be49fc50a6188240556114a6d89779b15b1fc6301a17e54b52586249aa64a98bbcaf62ad9cb55047d238aac95c10cf"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Implementation for the container extension.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-container-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-container-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-config-api",
      "purl": "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.config",
      "hashes": [
        { "alg": "MD5", "content": "d6e36f2fce3606dc359514b672e72f5e" },
        {
          "alg": "SHA-1",
          "content": "fa92a81486bbd354828f239a902dcc7b563593f4"
        },
        {
          "alg": "SHA-256",
          "content": "5f4ed74a3e9c72284f4eb1554fc8d1e64eec047b486fde87a41371eb362d4aac"
        },
        {
          "alg": "SHA-512",
          "content": "144229a7ea113b6779edbdc2b8ba7fed4f469340e9e8bdfc2febde684821c0feb26ec16a7ca4d74f31c98e77256b51630c8f114d2ab1a3ce973ebe0b21136349"
        },
        {
          "alg": "SHA-384",
          "content": "745fb50902a674a5cb5155abe611a16d645d9c5bc513a87ddafcecd5e58b64373fe986dc1f6a65f77746a1ad4ca8d5d2"
        },
        {
          "alg": "SHA3-384",
          "content": "54d0c7b8b923c9dcedb9ffe6801d3fe2e77033b739fefee41af567911d14fba6a57e7d3bd0205417dea339e3ad1cd168"
        },
        {
          "alg": "SHA3-256",
          "content": "13ab2019e850750586d92de643cf4d2651fb90587459b28d2744aabbf94a5ead"
        },
        {
          "alg": "SHA3-512",
          "content": "a44751eb36d103cc3b4b860af58372b009586d4d35ed6975322c57e5cc855b6f67a34a699e3403fb43dfee1c2eff8f14f550d8e3a75f9be125d3cf1e38481979"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "API for the Config Module.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-config-api",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-config-api",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-config-impl-base",
      "purl": "pkg:maven/org.jboss.arquillian.config/arquillian-config-impl-base@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.config",
      "hashes": [
        { "alg": "MD5", "content": "0bd1a0533a26a37a520c2b801530103d" },
        {
          "alg": "SHA-1",
          "content": "564eeff05f6b94563b167ac0d279c272aab8c6e1"
        },
        {
          "alg": "SHA-256",
          "content": "05bad1d7417b1b5d5d21f5c4e289fb77726e9af60a14f9f2a1053645ab302d31"
        },
        {
          "alg": "SHA-512",
          "content": "ccabf620eef3d5e18728f3c3a2ea5c29046ec8b51eafe3bc2b520bd6e644cab39f4fb4912244f1694d06043476fffc7d177747229733687c417d19b5440a604e"
        },
        {
          "alg": "SHA-384",
          "content": "ff3fbd397e3f6f5f7b03d53e5fe76dcbecbf69a6b630fa26805aec7e183304ee30286ee130cf814cab3834d41c52a292"
        },
        {
          "alg": "SHA3-384",
          "content": "a31032c5828f799ec5abb259eceaa3bab56db7a2a8b04dd7807c2ea753d18c755b8e4a602413e9af75ab2e740c1fdbab"
        },
        {
          "alg": "SHA3-256",
          "content": "5b9d5c44ee50f1a2125f76c588475ad43753fb3551a5f7ab78c5da241e5dca96"
        },
        {
          "alg": "SHA3-512",
          "content": "3ef6dd43f84e3249c0dc16a5a13c7bded205a74225ec63c54ac62891f7ba9eac4b3c85beee96bb91b06e518fd580b0603a5983d49d0cfbd243cf959274b97d78"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-impl-base@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Implementation for the Config module.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-config-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-config-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-config-spi",
      "purl": "pkg:maven/org.jboss.arquillian.config/arquillian-config-spi@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.config",
      "hashes": [
        { "alg": "MD5", "content": "e2696e1c36d82702389d195d66b435b5" },
        {
          "alg": "SHA-1",
          "content": "7ecb54aab850b1faf45838226a93e559de6c29b0"
        },
        {
          "alg": "SHA-256",
          "content": "50be91e329ee80d9168a93683ca8d4d2705743a8f4e770f4e530394f2d5f51fc"
        },
        {
          "alg": "SHA-512",
          "content": "364a8abb416e7911f3d41bfda72c235f5a941bbd2a3a5ddde3657af2d7db71b44ae8a939e801bfcee7a5238982eb65e537f29280ffefbd40ed402d6c62ae662c"
        },
        {
          "alg": "SHA-384",
          "content": "b9cac5f6501ab7b697386c23015983415f894fed02ec146ad7af0c9ac799dcd4f519b312d6e862795735fdadd7efc61f"
        },
        {
          "alg": "SHA3-384",
          "content": "65e98de239e28e5cafaa7c76fb48e7dd940b88dbea041e5c5f5d76f6bed07f6ba97f5f9cd9af51668c58ed536564371f"
        },
        {
          "alg": "SHA3-256",
          "content": "aa040ad47a66a43015f2af693836955d674bda1e959dc12a7ffcb74ea15ffb5a"
        },
        {
          "alg": "SHA3-512",
          "content": "0204adad130cc0c3501c14b50190a97769d8cf7dbd6df59bf11f576036574db366331594886a1dbd1ff79c8c767e2769a0da0e4fe16785929bb8ae540b188cf1"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-spi@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "SPI for the Config Module.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-config-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-config-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "shrinkwrap-descriptors-spi",
      "purl": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi@2.0.0?type=jar",
      "type": "library",
      "group": "org.jboss.shrinkwrap.descriptors",
      "hashes": [
        { "alg": "MD5", "content": "42e9f8d85faf58d2820280b7de474592" },
        {
          "alg": "SHA-1",
          "content": "d080f5e818c9fe61dd35fb9134dbc2583f5b0369"
        },
        {
          "alg": "SHA-256",
          "content": "2171060298bca314c9b7629c89e065bbd5c911a6069bf1d12323280128574e87"
        },
        {
          "alg": "SHA-512",
          "content": "ed1ca915ca5c89f9ecb6cbcc8013e4df1d6c25ec7f3a66c2b3ec8599e806f6d0cec677cc511e847e11875bb1dd9ad668bf534a5fbf22a493e407228fc0f31d4e"
        },
        {
          "alg": "SHA-384",
          "content": "c10c32f1be12dd45e69ecdba1ef864270df34244546a66e465ddb9b9753e67d3103a6f256aa043f9e82a0cbe7aa2a22a"
        },
        {
          "alg": "SHA3-384",
          "content": "0179148237726f9cde6b97a747aca5e7aeb5e58e3d35ba14cfee0d7ee6e5f27bb6201ddfafde6cfb070c1658d007898d"
        },
        {
          "alg": "SHA3-256",
          "content": "adb9ee44790cdb726eedbef29f3e81c983c4d4d51e84054060f10b4effd1b8b0"
        },
        {
          "alg": "SHA3-512",
          "content": "77f198356c383a37c613952438163da24fec6ac3d34c3e4a53a8b37e9ae3dbd89038ca710a90e1c91093a8a8e7d416e236de6def0e7669dd0c1e01a12e75c5a9"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi@2.0.0?type=jar",
      "version": "2.0.0",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Service Provider Interface of the ShrinkWrap Descriptors Project",
      "externalReferences": [
        {
          "url": "http://www.jboss.org/shrinkwrap-descriptors-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/SHRINKDESC",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "http://github.com/shrinkwrap/descriptors/shrinkwrap-descriptors-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-container-test-impl-base",
      "purl": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-impl-base@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.container",
      "hashes": [
        { "alg": "MD5", "content": "f64c92f6b955d32d7b17c4b986ea8630" },
        {
          "alg": "SHA-1",
          "content": "6cd742ab9e0192ad9ce3af4d77f247954ed5d7c7"
        },
        {
          "alg": "SHA-256",
          "content": "93a93ff4bd9e68f3fccbf0a062a071e67075364ec2add7278e4b9546329b16d9"
        },
        {
          "alg": "SHA-512",
          "content": "4292388f6513ce6a07a7355ab04506ca294b300e839fff2a8e7baaab072abb79c29e108fee696298504b0a221051cc6d35e2b09cd9ed683f5e79a01cc62eab4e"
        },
        {
          "alg": "SHA-384",
          "content": "7235a98772b140148d7e30558b3fcd393202a5d3b85160149a63943f45cf5868d1e29623866c9a1479f2bd4d3bb9966b"
        },
        {
          "alg": "SHA3-384",
          "content": "923187f375f90f258d4a99f05e9dfd6d613de08d5689e9d03b83a3920a2916c5cda762871d31fea5390e14d26608325f"
        },
        {
          "alg": "SHA3-256",
          "content": "ba4f591fc2d87b034f9725962f48edc83c63eeb4f1305e0891701b76d22973f1"
        },
        {
          "alg": "SHA3-512",
          "content": "6d754115a8fe6e65351a15c4a9275da25fdefdb8777b03ee9bfda2c50ec8acab85d5eb688cd224991e9c7de2006165f05e0dbf884886d7fa084be9a1efa59c5a"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-impl-base@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Integration with the Test extention for the container extension.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-container-test-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-container-test-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "shrinkwrap-impl-base",
      "purl": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-impl-base@1.2.6?type=jar",
      "type": "library",
      "group": "org.jboss.shrinkwrap",
      "hashes": [
        { "alg": "MD5", "content": "bb03bc1f98cd217ac2965a9ceb4ff56e" },
        {
          "alg": "SHA-1",
          "content": "ad8a42acf8404b9dbdfb586a011a34cc353c2818"
        },
        {
          "alg": "SHA-256",
          "content": "7b92757d599d4f21efd70182981efdc193d66b03312a5d293ed6ab10de646ae2"
        },
        {
          "alg": "SHA-512",
          "content": "60a512b58bffee90687f434bd5c54497e1b895319bd0800ef06da199713d45ba0ba9d4a1738c523905926c23cf4cd94709f4542088cabd45e6c5d96395a732db"
        },
        {
          "alg": "SHA-384",
          "content": "f7ab5c57c30ceaab9b59b6290595b57ee9e45dfd875ea5c8ead7e3cceb6661b3ff6d82ea5865d469e22b137cd5dcad32"
        },
        {
          "alg": "SHA3-384",
          "content": "f08d298365fee6b9fc29bb191cd18713dbe988dd9dddbf060f3e8feece71917bd6d4dc71d9cc798e459c64d492543b6f"
        },
        {
          "alg": "SHA3-256",
          "content": "4ae33924a8e9197775148759170e66f1c7b3427ae3acaeca5b1c381bc0658df1"
        },
        {
          "alg": "SHA3-512",
          "content": "091f1d90baea5ade33cc31cd19462ef78bb133449ec9aa5297a9d06eb2e4c99cc0fadbd5a8f80df650256425939d6f13b65b4414e1c0815f699bbdac400a42c1"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-impl-base@1.2.6?type=jar",
      "version": "1.2.6",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Common Base for Implementations of the ShrinkWrap Project",
      "externalReferences": [
        {
          "url": "http://www.jboss.org/shrinkwrap-impl-base",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/SHRINKWRAP",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/shrinkwrap/shrinkwrap/shrinkwrap-impl-base",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "shrinkwrap-spi",
      "purl": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-spi@1.2.6?type=jar",
      "type": "library",
      "group": "org.jboss.shrinkwrap",
      "hashes": [
        { "alg": "MD5", "content": "de56903edf6c0a6ea49646d330012894" },
        {
          "alg": "SHA-1",
          "content": "2f5f82d975335e64793684452604d9e8edb433ae"
        },
        {
          "alg": "SHA-256",
          "content": "c0e674c1461e18cfa3c24789fb4420476962e2f0e379d1b4bfe8f72bc99d1348"
        },
        {
          "alg": "SHA-512",
          "content": "9cb045479d169c97b10cf4e46c8e5e7310c7e4e8951da222d04144d111aa6c91d3e6015a3961df09ffccddc788bbe10a241eb5e26c1142d3b4743b04f02d90fb"
        },
        {
          "alg": "SHA-384",
          "content": "6e4e07699ec2aa010c71c2395bf1e79302e1edc96af5128eed16b02a92b45533dd26f830b63a6f81351ffb6168457360"
        },
        {
          "alg": "SHA3-384",
          "content": "19aab9fdb99c823ac7b32de6bf75727c88f2b24313749d2148d2930dcdbcdc3490d26f36d55103d4e501512ab1be2d8e"
        },
        {
          "alg": "SHA3-256",
          "content": "b23de3de29a0e11533bf99c3b5580409526d7aad7174c2d58e2b7e903a341476"
        },
        {
          "alg": "SHA3-512",
          "content": "1dd9385e4ae1f2e19ccbb0366111df98e5fd258f0ffe0c6f0ef0800d3c955adeeacd8207e448389050b4b4021cf3b3d64776bef4cd6b9ce69398f263a5fdc235"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-spi@1.2.6?type=jar",
      "version": "1.2.6",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Generic Service Provider Contract of the ShrinkWrap Project",
      "externalReferences": [
        { "url": "http://www.jboss.org/shrinkwrap-spi", "type": "website" },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/SHRINKWRAP",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/shrinkwrap/shrinkwrap/shrinkwrap-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-container-test-spi",
      "purl": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.container",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "c6f98bc827b216a711c16f7ca767ad65" },
        {
          "alg": "SHA-1",
          "content": "704aa9244a80fbe525dd9f5953a477888458b8df"
        },
        {
          "alg": "SHA-256",
          "content": "0ce601385aafb1d304874f5031b67cc2cef0f1085ea6a3c494d6cdbc6331852b"
        },
        {
          "alg": "SHA-512",
          "content": "b69692befb3e3f142ad1a7329e98ee7b1b74e7f34e4de15e1b475828e839bc7f424439625bcaa14e76dc090e3b7f406487ae6bf92a2a1a9d721ee9f4eeb878a8"
        },
        {
          "alg": "SHA-384",
          "content": "655ec1508023a180498d3cdb285d3a03930ab1198ad925843146a14ed5a55b905a612b463eaf5dd27dfcf455b7c0e0ce"
        },
        {
          "alg": "SHA3-384",
          "content": "a747a81423f9e4857985e87f77d9458a69d042c3b24ac2754895a75610a6872d16b8771ebd27fd2fa89357dc76b49ffd"
        },
        {
          "alg": "SHA3-256",
          "content": "efe23f2a68a5f2d4af4697c7a29cc83b5adf66b04aff6fccf88a4fcfd2bce31d"
        },
        {
          "alg": "SHA3-512",
          "content": "056a7e155375202ec9b88fd8949f6df16125d23d9201b184d20e05f1f68bd2fd4c8176245756289999be0fef99bb8b11dc0f601ed96b4ed5441b2c95dc2b2633"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Integration with the Test extention for the container extension.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-container-test-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-container-test-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "arquillian-container-spi",
      "purl": "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
      "type": "library",
      "group": "org.jboss.arquillian.container",
      "hashes": [
        { "alg": "MD5", "content": "c6a23b5b48cb3f4b9c3db58f7e32877e" },
        {
          "alg": "SHA-1",
          "content": "8220fd59f54d27576b6035ac692efe843e0c8c9b"
        },
        {
          "alg": "SHA-256",
          "content": "c76b2f3e38b184b51ec8ca3dc95d5f240e69f2de73b42527693a39f8b531542a"
        },
        {
          "alg": "SHA-512",
          "content": "c575d51a0c32a091848d0f5831c178825f04c1df1eebada2001acda4e89b81f4c77b6b4494ba0f3d0b8844c0386e4d2a6a8b410b1d4ba37425354273c3c46f5f"
        },
        {
          "alg": "SHA-384",
          "content": "25ff46ea654b4d0db98ab90204da7cb10a53be5cc82412951fd31a8beb54912cd2a84718bc3130b4ff5658032416d598"
        },
        {
          "alg": "SHA3-384",
          "content": "edf7dadda72ec7caf1d72108b25c28cb8a2aa0e6ebd6b929a3fa077631951038d976ea4ed6c5314c93965d6103e9a6d5"
        },
        {
          "alg": "SHA3-256",
          "content": "16c309011560c47a78712b3201d187b8b05c652c1ff0bf844cd4c5b24937de5a"
        },
        {
          "alg": "SHA3-512",
          "content": "f2e79e26c7bf3f0edf80ef599cec3c95f83cef1d9f5f7ff2b201044cf759065ba5e8ca348b256bc37d4cad0a3376bac08220cf560948a8180dc83cafc3372238"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
      "version": "1.6.0.Final",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "SPI for the container extension.",
      "externalReferences": [
        {
          "url": "http://arquillian.org/arquillian-build/arquillian-container-spi",
          "type": "website"
        },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/ARQ",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "git://github.com/arquillian/arquillian-core.git/arquillian-build/arquillian-container-spi",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "shrinkwrap-api",
      "purl": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
      "type": "library",
      "group": "org.jboss.shrinkwrap",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "25ec1381d61c9d7e4d10c825fae1fca1" },
        {
          "alg": "SHA-1",
          "content": "39916427fc0d7f0db64499e36b0f3bdd97c0ee1b"
        },
        {
          "alg": "SHA-256",
          "content": "84895690e1ae4d988693ff18acd069141602a5c869aaca19b2d027291f8f0bdd"
        },
        {
          "alg": "SHA-512",
          "content": "3274595ce7f02dfa65047d51ead4988a8b37f3861bb8af7448c637723f0bb6966976ec2e6a508491f53fe6fdbdf154d6773b3fb748eb8cf2b3d36646706ed290"
        },
        {
          "alg": "SHA-384",
          "content": "4d0e4440c8d0884e5a3d25e93f2b6bb892d2b5fd7f04a03c5945da8274a0c3cfb4dced514f7662b39bfa0d83b5a75fa5"
        },
        {
          "alg": "SHA3-384",
          "content": "cb978697a0ea84a919193eccc343374f168af3314ffb66bed5279d4474b678351c9b174a6595e97a3c574af93ce6aad2"
        },
        {
          "alg": "SHA3-256",
          "content": "7c08724949a5442b79fb22b119c561c1deaea16aac66c9b624f0725d8677bfa2"
        },
        {
          "alg": "SHA3-512",
          "content": "2b582d62e0e1d0969510e9a5e2494da02a392820747511aed89c7819e9157fecc98975c64ab45c92bdf0a56e07536622858588d309667f3e41a62b50b5ec1aa0"
        }
      ],
      "bom-ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
      "version": "1.2.6",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "JBoss by Red Hat",
      "description": "Client View of the ShrinkWrap Project",
      "externalReferences": [
        { "url": "http://www.jboss.org/shrinkwrap-api", "type": "website" },
        {
          "url": "https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/",
          "type": "distribution"
        },
        {
          "url": "http://jira.jboss.com/jira/browse/SHRINKWRAP",
          "type": "issue-tracker"
        },
        {
          "url": "http://lists.jboss.org/pipermail/jboss-user/",
          "type": "mailing-list"
        },
        {
          "url": "https://github.com/shrinkwrap/shrinkwrap/shrinkwrap-api",
          "type": "vcs"
        }
      ]
    },
    {
      "name": "commons-io",
      "purl": "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
      "type": "library",
      "group": "commons-io",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "9165992827666a785f92109d4173d87f" },
        {
          "alg": "SHA-1",
          "content": "a5e188b576968d37809c5cac229aede55a70413b"
        },
        {
          "alg": "SHA-256",
          "content": "8f6b43490a3c60f33046f7b9cd0f5d96abccf1b068fba395dd58759d16dd930c"
        },
        {
          "alg": "SHA-512",
          "content": "24ff256ae5249cd133ea01636683129bc32eb8527071686580c983a94a19c23d474a78fb3a410a4503d3215d0ab89bd083701e281ea37991430918afbec63c7b"
        },
        {
          "alg": "SHA-384",
          "content": "c675984aabb80ec9d6a8b24471b4db507b07e2205c008e724fce85b65a9a9451a98bcab97521e9a229a69811a9b28db0"
        },
        {
          "alg": "SHA3-384",
          "content": "f736e2a3dabe359881518bed08cbf7ae025e41835d3b431d92f3af92ac84fc9b4e33492e4b122e2cc04032692df3098d"
        },
        {
          "alg": "SHA3-256",
          "content": "92ed839dc067df27d500ec2529e94ab7c5c6f8b9dd231cfb270c8d320dca0f3e"
        },
        {
          "alg": "SHA3-512",
          "content": "0f69b618d4142fbf4a124c85c9c172835126ea249c2112800fc320909c47c6e682efbef768535680c5fad1d8a73d86c41c14bab9663f58bd3687f30d538c520a"
        }
      ],
      "bom-ref": "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
      "version": "2.6.0.redhat-00001",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "pedigree": {
        "commits": [
          {
            "uid": "ca5e4e0157eb026c1fc3f5761afd3f5440f25985",
            "url": "http://code.engineering.redhat.com/gerrit/apache/commons-io.git#2.6.0.redhat-00001"
          }
        ]
      },
      "supplier": { "url": ["https://www.redhat.com"], "name": "Red Hat" },
      "publisher": "Red Hat",
      "description": "The Apache Commons IO library contains utility classes, stream implementations, file filters, file comparators, endian transformation classes, and much more.",
      "externalReferences": [
        {
          "url": "http://commons.apache.org/proper/commons-io/",
          "type": "website"
        },
        {
          "url": "https://orch.psi.redhat.com/pnc-rest/v2/builds/32374",
          "type": "build-system",
          "comment": "pnc-build-id"
        },
        {
          "url": "http://issues.apache.org/jira/browse/IO",
          "type": "issue-tracker"
        },
        {
          "url": "http://mail-archives.apache.org/mod_mbox/commons-user/",
          "type": "mailing-list"
        },
        {
          "url": "https://git-wip-us.apache.org/repos/asf?p=commons-io.git",
          "type": "vcs"
        },
        {
          "url": "https://maven.repository.redhat.com/ga/",
          "type": "distribution"
        },
        {
          "url": "quay.io/rh-newcastle/builder-rhel-7-j8-mvn3.3.9:1.0.0",
          "type": "build-meta",
          "comment": "pnc-environment-image"
        }
      ]
    },
    {
      "name": "jsonassert",
      "purl": "pkg:maven/org.skyscreamer/jsonassert@1.5.0?type=jar",
      "type": "library",
      "group": "org.skyscreamer",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "9139a7742b3a752179d233f551b145ae" },
        {
          "alg": "SHA-1",
          "content": "6c9d5fe2f59da598d9aefc1cfc6528ff3cf32df3"
        },
        {
          "alg": "SHA-256",
          "content": "a310bc79c3f4744e2b2e993702fcebaf3696fec0063643ffdc6b49a8fb03ef39"
        },
        {
          "alg": "SHA-512",
          "content": "ce2100374f56027c950df4bcfe10987fe46905690db83393504a2198761429594a9e7d51743eeed102a9f6584c7032e87c4bbe262e1a4d5591b8692b76e594de"
        },
        {
          "alg": "SHA-384",
          "content": "380b2ec0b8aad4b2c9081607d913492b571ed7923258fd917e566e4eeccac346522aa5bce1aa1193fa4b0b7dc9d6f32c"
        },
        {
          "alg": "SHA3-384",
          "content": "4f780a5931f9da14e44f7afff39d7e6de5fda5c8a62ea20c66930ac98c28e355962a663fb17267470926db02ceccb5ec"
        },
        {
          "alg": "SHA3-256",
          "content": "7600fe163ba3d2db2635a9f4ae368d0326f846056f6cf87a7c21029df6b23e29"
        },
        {
          "alg": "SHA3-512",
          "content": "77d0db9667fc36e73616bc2bb592145c9f2d3a62aa33c33bc4e3bb2fc1f817675ebaddcedb5644b50270b044b790dd997224e9a67f2e57e8d465dba4f1ab2a0d"
        }
      ],
      "bom-ref": "pkg:maven/org.skyscreamer/jsonassert@1.5.0?type=jar",
      "version": "1.5.0",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "description": "A library to develop RESTful but flexible APIs",
      "externalReferences": [
        {
          "url": "https://github.com/skyscreamer/JSONassert",
          "type": "website"
        },
        {
          "url": "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
          "type": "distribution"
        }
      ]
    },
    {
      "name": "android-json",
      "purl": "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar",
      "type": "library",
      "group": "com.vaadin.external.google",
      "hashes": [
        { "alg": "MD5", "content": "10612241a9cc269501a7a2b8a984b949" },
        {
          "alg": "SHA-1",
          "content": "fa26d351fe62a6a17f5cda1287c1c6110dec413f"
        },
        {
          "alg": "SHA-256",
          "content": "dfb7bae2f404cfe0b72b4d23944698cb716b7665171812a0a4d0f5926c0fac79"
        },
        {
          "alg": "SHA-512",
          "content": "c4a06a0a3ce7bdbee702c06944265c050a4c8d2fbd21c248936e2edfdab63acea30f2cf3568d3c21a559940d939985a8b10d30aff972a3e8cbeb392c0b02da3a"
        },
        {
          "alg": "SHA-384",
          "content": "60d1044b5439cdf5eb621118cb0581365ab4f023a30998b238b87854236f03d8395d45b0262fb812335ff904cb77f25f"
        },
        {
          "alg": "SHA3-384",
          "content": "b80ebdbec2127279ca402ca52e50374d3ca773376258f6aa588b442822ee7362de8cca206db71b79862bde84018cf450"
        },
        {
          "alg": "SHA3-256",
          "content": "6285b1ac8ec5fd339c7232affd9c08e6daf91dfa18ef8ae7855f52281d76627e"
        },
        {
          "alg": "SHA3-512",
          "content": "de7ed83f73670213b4eeacfd7b3ceb7fec7d88ac877f41aeaacf43351d04b34572f2edc9a8f623af5b3fccab3dac2cc048f5c8803c1d4dcd1ff975cd6005124d"
        }
      ],
      "bom-ref": "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar",
      "version": "0.0.20131108.vaadin1",
      "licenses": [
        {
          "license": {
            "id": "Apache-2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0"
          }
        }
      ],
      "description": "??JSON (JavaScript Object Notation) is a lightweight data-interchange format. This is the org.json compatible Android implementation extracted from the Android SDK ?",
      "externalReferences": [
        { "url": "http://developer.android.com/sdk", "type": "website" },
        {
          "url": "http://oss.sonatype.org/content/repositories/vaadin-releases/",
          "type": "distribution"
        },
        { "url": "http://developer.android.com/sdk/", "type": "vcs" }
      ]
    },
    {
      "name": "geronimo-annotation_1.2_spec",
      "purl": "pkg:maven/org.apache.geronimo.specs/geronimo-annotation_1.2_spec@1.0?type=jar",
      "type": "library",
      "group": "org.apache.geronimo.specs",
      "scope": "optional",
      "hashes": [
        { "alg": "MD5", "content": "b2f9dc0810cd957672f88857b45a54d4" },
        {
          "alg": "SHA-1",
          "content": "25aaae4a0ec554b70d6d88110b23e49972193fc9"
        },
        {
          "alg": "SHA-256",
          "content": "7c38526e957023f57c740f4ccd1d5662a00fc0cb9e17982d4b15bf3f0b29813f"
        },
        {
          "alg": "SHA-512",
          "content": "bc8a89304db84fffedd00797e22c2d2a52fa74d17449f83443101868d094e3d75ea28abf310c6f527502b9a450474a306df3dc969cdbbe091602b560f40982be"
        },
        {
          "alg": "SHA-384",
          "content": "5e9ca4c1db0a79de3d3a6385180c291138cec90bd63504ff14b61d153b8cd2a53c48a2869dac13138b9c47b44869049f"
        },
        {
          "alg": "SHA3-384",
          "content": "cdb337f04eb63d3b70cf7cbf4a1293ff38532910c66f4945ee6a1bdbf51f22000650316e95431165d43ec998f4581804"
        },
        {
          "alg": "SHA3-256",
          "content": "648c043f1f913b4e8befc849d9c33b0283e6e8007427785f636ceaed69a487bf"
        },
        {
          "alg": "SHA3-512",
          "content": "28983cd0ffef3d11aae7c04357422b90ced444a0d5a165d22ec376a8ecd6bbc3048da0f5a11503e84683c3253c6df62d63487f59589c2826b1d7316e07a176e6"
        }
      ],
      "bom-ref": "pkg:maven/org.apache.geronimo.specs/geronimo-annotation_1.2_spec@1.0?type=jar",
      "version": "1.0",
      "licenses": [{ "license": { "id": "Apache-2.0" } }],
      "publisher": "The Apache Software Foundation",
      "description": "Annotation spec 1.2 API",
      "externalReferences": [
        {
          "url": "http://geronimo.apache.org/maven/specs/geronimo-annotation_1.2_spec/1.0",
          "type": "website"
        },
        {
          "url": "https://repository.apache.org/service/local/staging/deploy/maven2",
          "type": "distribution"
        },
        {
          "url": "https://issues.apache.org/jira/browse/GERONIMO",
          "type": "issue-tracker"
        },
        {
          "url": "http://mail-archives.apache.org/mod_mbox/geronimo-user",
          "type": "mailing-list"
        },
        {
          "url": "http://svn.apache.org/viewcvs.cgi/geronimo/specs/tags/geronimo-annotation_1.2_spec-1.0",
          "type": "vcs"
        }
      ]
    }
  ],
  "specVersion": "1.4",
  "dependencies": [
    {
      "ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
      "dependsOn": [
        "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
        "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
        "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-tck@1.1.0.redhat-00008?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
      "dependsOn": [
        "pkg:maven/org.osgi/org.osgi.annotation.versioning@1.1.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.osgi/org.osgi.annotation.versioning@1.1.0?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-tck@1.1.0.redhat-00008?type=jar",
      "dependsOn": [
        "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
        "pkg:maven/jakarta.enterprise/jakarta.enterprise.cdi-api@2.0.2.redhat-00004?type=jar",
        "pkg:maven/jakarta.json.bind/jakarta.json.bind-api@1.0.2.redhat-00003?type=jar",
        "pkg:maven/jakarta.json/jakarta.json-api@1.1.6.redhat-00003?type=jar",
        "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-container@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.testng/testng@7.0.0?type=jar",
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
        "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
        "pkg:maven/org.skyscreamer/jsonassert@1.5.0?type=jar",
        "pkg:maven/org.apache.geronimo.specs/geronimo-annotation_1.2_spec@1.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/jakarta.enterprise/jakarta.enterprise.cdi-api@2.0.2.redhat-00004?type=jar",
      "dependsOn": [
        "pkg:maven/jakarta.interceptor/jakarta.interceptor-api@1.2.5.redhat-00002?type=jar",
        "pkg:maven/jakarta.inject/jakarta.inject-api@1.0.0.redhat-00002?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/jakarta.interceptor/jakarta.interceptor-api@1.2.5.redhat-00002?type=jar",
      "dependsOn": [
        "pkg:maven/jakarta.annotation/jakarta.annotation-api@1.3.5.redhat-00002?type=jar",
        "pkg:maven/jakarta.ejb/jakarta.ejb-api@3.2.6?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/jakarta.annotation/jakarta.annotation-api@1.3.5.redhat-00002?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/jakarta.ejb/jakarta.ejb-api@3.2.6?type=jar",
      "dependsOn": [
        "pkg:maven/jakarta.transaction/jakarta.transaction-api@1.3.2?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/jakarta.transaction/jakarta.transaction-api@1.3.2?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/jakarta.inject/jakarta.inject-api@1.0.0.redhat-00002?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/jakarta.json.bind/jakarta.json.bind-api@1.0.2.redhat-00003?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/jakarta.json/jakarta.json-api@1.1.6.redhat-00003?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-container@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-core@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-impl-base@1.2.6?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.testng/arquillian-testng-core@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-impl-base@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.config/arquillian-config-spi@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi@2.0.0?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-api-base@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.core/arquillian-core-impl-base@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.test/arquillian-test-impl-base@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-spi@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-impl-base@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.core/arquillian-core-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.config/arquillian-config-impl-base@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.shrinkwrap.descriptors/shrinkwrap-descriptors-spi@2.0.0?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-impl-base@1.6.0.Final?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-spi@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.test/arquillian-test-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-api@1.6.0.Final?type=jar",
        "pkg:maven/org.jboss.arquillian.container/arquillian-container-test-spi@1.6.0.Final?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-impl-base@1.2.6?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar",
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-spi@1.2.6?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-spi@1.2.6?type=jar",
      "dependsOn": [
        "pkg:maven/org.jboss.shrinkwrap/shrinkwrap-api@1.2.6?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/org.testng/testng@7.0.0?type=jar",
      "dependsOn": ["pkg:maven/com.beust/jcommander@1.72?type=jar"]
    },
    {
      "ref": "pkg:maven/com.beust/jcommander@1.72?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.skyscreamer/jsonassert@1.5.0?type=jar",
      "dependsOn": [
        "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar",
      "dependsOn": []
    },
    {
      "ref": "pkg:maven/org.apache.geronimo.specs/geronimo-annotation_1.2_spec@1.0?type=jar",
      "dependsOn": []
    }
  ],
  "serialNumber": "urn:uuid:a3cdb09e-6c89-4a3c-bb2e-a3b4899edf72"
}
'
	);

INSERT INTO sbom_generation_request(
		id,
		creation_time,
		identifier,
		status,
		result,
		reason,
		config,
    type
	)
VALUES (
		'OPAASSDDFF',
		'2023-12-25T00:00:00.000000Z',
		'OPBGCD23DVYAC',
		'FINISHED',
		'SUCCESS',
		'',
		'{
  "apiVersion": "sbomer.jboss.org/v1alpha1",
  "type": "operation",
  "operationId": "OPBGCD23DVYAC",
  "product": {
    "processors": [
      {
        "type": "redhat-product",
        "errata": {
          "productName": "RHBQ",
          "productVariant": "8Base-RHBQ-2.13",
          "productVersion": "RHEL-8-RHBQ-2.13"
        }
      }
    ],
    "generator": {
      "type": "cyclonedx-operation"
    }
  }
}',
  'OPERATION'
	);

INSERT INTO sbom(
		id,
		root_purl,
		creation_time,
		identifier,
		generationRequest_id,
		config_index,
		sbom
	)
VALUES (
		'816640206274228223',
		'pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC',
		'2023-12-25T00:00:00.000000Z',
		'OPBGCD23DVYAC',
		'OPAASSDDFF',
		0,
    '{
  "bomFormat": "CycloneDX",
  "specVersion": "1.4",
  "version": 1,
  "metadata": {
    "timestamp": "2024-02-14T17:42:25Z",
    "tools": [
      {
        "vendor": "Red Hat",
        "name": "sbomer",
        "version": "1.0.0"
      }
    ],
    "licenses": [
      {
        "license": {
          "id": "Apache-2.0"
        }
      }
    ],
    "properties": [
      {
        "name": "vcs",
        "value": "git@github.com:project-ncl/sbomer.git"
      },
      {
        "name": "website",
        "value": "https://github.com/project-ncl/sbomer"
      }
    ],
    "component": {
      "name": "my-broker-7.11.5.CR3-bin.zip",
      "version": "7.11.5.CR3",
      "description": "SBOM representing the deliverable my-broker-7.11.5.CR3-bin.zip analyzed with operation OPBGCD23DVYAC",
      "licenses": [

      ],
      "purl": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "type": "file",
      "bom-ref": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "http://orch.com/pnc-rest/v2/operations/deliverable-analyzer/OPBGCD23DVYAC",
          "comment": "pnc-operation-id"
        }
      ]
    }
  },
  "components": [
    {
      "group": "com.google.errorprone",
      "name": "error_prone_annotations",
      "version": "2.2.0",
      "scope": "required",
      "hashes": [
        {
          "alg": "MD5",
          "content": "416757b9e6ba0563368ab59e668b3225"
        },
        {
          "alg": "SHA-1",
          "content": "88e3c593e9b3586e1c6177f89267da6fc6986f0c"
        },
        {
          "alg": "SHA-256",
          "content": "6ebd22ca1b9d8ec06d41de8d64e0596981d9607b42035f9ed374f9de271a481a"
        }
      ],
      "purl": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "https://brewweb.com/buildinfo?buildID=649279",
          "comment": "brew-build-id"
        }
      ],
      "type": "library",
      "bom-ref": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar"
    },
    {
      "group": "org.codehaus.mojo",
      "name": "animal-sniffer-annotations",
      "version": "1.17",
      "scope": "required",
      "hashes": [
        {
          "alg": "MD5",
          "content": "7ca108b790cf6ab5dbf5422cc79f0d89"
        },
        {
          "alg": "SHA-1",
          "content": "f97ce6decaea32b36101e37979f8b647f00681fb"
        },
        {
          "alg": "SHA-256",
          "content": "92654f493ecfec52082e76354f0ebf87648dc3d5cec2e3c3cdb947c016747a53"
        }
      ],
      "purl": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "https://brewweb.com/buildinfo?buildID=848654",
          "comment": "brew-build-id"
        }
      ],
      "type": "library",
      "bom-ref": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar"
    }
  ],
  "dependencies": [
    {
      "ref": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "dependsOn": [
        "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
        "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
      "dependsOn": [

      ]
    },
    {
      "ref": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar",
      "dependsOn": [

      ]
    }
  ]
}');

-- NEXTGEN example content

INSERT INTO generation(
		id,
		created,
        updated,
        finished,
        request,
		status,
        result,
        reason
	)
VALUES (
        'G1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '{"target":{"identifier":"quay.io/pct-security/mequal:latest","type":"CONTAINER_IMAGE"},"generator":{"name":"syft","version":"1.26.1","config":{"format":"CYCLONEDX_1.6_JSON","resources":{"requests":{"cpu":"100m","memory":"300Mi"},"limits":{"cpu":"1000m","memory":"1Gi"}},"options":{}}}}',
        'FINISHED',
        'SUCCESS',
        'Generation successfully finished'
);

INSERT INTO generation_status_history(
		id,
        generation_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S1AAAAA',
        'G1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'NEW',
        'Initial state'
);

INSERT INTO generation_status_history(
		id,
        generation_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S2AAAAA',
        'G1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'SCHEDULED',
        'WAiting to be picked up'
);

INSERT INTO generation_status_history(
		id,
        generation_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S3AAAAA',
        'G1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'GENERATING',
        'In progress'
);

INSERT INTO generation_status_history(
		id,
        generation_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S4AAAAA',
        'G1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'FINISHED',
        'Generation successfully finished'
);

INSERT INTO event(
		id,
		created,
        updated,
        finished,
        metadata,
		status,
        reason
	)
VALUES (
        'E0AAAAA',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '{"type":"container_image", "website":"https://example.com", "source":"REST:/api/v1beta2/generations"}',
        'NEW',
        'Event processed'
);

INSERT INTO event(
		id,
		created,
        updated,
        finished,
        metadata,
		status,
        reason
	)
VALUES (
        'E0BBBBB',
        '2020-12-25T00:00:00.000000Z',
        '2022-12-25T00:00:00.000000Z',
        '2024-12-25T00:00:00.000000Z',
        '{"type":"build"}',
        'RESOLVED',
        'Some reason'
);

INSERT INTO event(
		id,
		created,
        updated,
        finished,
        metadata,
		status,
        reason
	)
VALUES (
        'E0CCCCC',
        '2020-12-25T00:00:00.000000Z',
        '2022-12-25T00:00:00.000000Z',
        '2024-12-25T00:00:00.000000Z',
        '{"type":"repository"}',
        'NEW',
        'Some reason'
);

INSERT INTO event(
		id,
        parent_id,
		created,
        updated,
        finished,
        metadata,
		status,
        reason
	)
VALUES (
        'E1AAAAA',
        'E0AAAAA',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '2023-12-25T00:00:00.000000Z',
        '{}',
        'PROCESSED',
        'Event processed'
);

INSERT INTO event_generation(
		event_id,
		generation_id
	) VALUES (
		'E1AAAAA',
		'G1AAAAA'
);

INSERT INTO event_status_history(
		id,
        event_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S1AAAAA',
        'E1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'NEW',
        'Initial state'
);

INSERT INTO event_status_history(
		id,
        event_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S2AAAAA',
        'E1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'RESOLVING',
        'Being resolved'
);

INSERT INTO event_status_history(
		id,
        event_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S3AAAAA',
        'E1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'RESOLVED',
        ''
);

INSERT INTO event_status_history(
		id,
        event_id,
		timestamp,
		status,
        reason
	)
VALUES (
        'S4AAAAA',
        'E1AAAAA',
        '2023-12-25T00:00:00.000000Z',
        'PROCESSED',
        'Event successfully finished'
);

INSERT INTO manifest(
		id,
		created,
        generation_id,
        metadata,
		bom
	)
VALUES (
		'816640206274228223',
		'2023-12-25T00:00:00.000000Z',
        'G1AAAAA',
        '{"sha256": "0f632851ad89b7897c37020326fe511f608b36b1124143376d4d413522612c6f"}',
    '{
  "bomFormat": "CycloneDX",
  "specVersion": "1.4",
  "version": 1,
  "metadata": {
    "timestamp": "2024-02-14T17:42:25Z",
    "tools": [
      {
        "vendor": "Red Hat",
        "name": "sbomer",
        "version": "1.0.0"
      }
    ],
    "licenses": [
      {
        "license": {
          "id": "Apache-2.0"
        }
      }
    ],
    "properties": [
      {
        "name": "vcs",
        "value": "git@github.com:project-ncl/sbomer.git"
      },
      {
        "name": "website",
        "value": "https://github.com/project-ncl/sbomer"
      }
    ],
    "component": {
      "name": "my-broker-7.11.5.CR3-bin.zip",
      "version": "7.11.5.CR3",
      "description": "SBOM representing the deliverable my-broker-7.11.5.CR3-bin.zip analyzed with operation OPBGCD23DVYAC",
      "licenses": [

      ],
      "purl": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "type": "file",
      "bom-ref": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "http://orch.com/pnc-rest/v2/operations/deliverable-analyzer/OPBGCD23DVYAC",
          "comment": "pnc-operation-id"
        }
      ]
    }
  },
  "components": [
    {
      "group": "com.google.errorprone",
      "name": "error_prone_annotations",
      "version": "2.2.0",
      "scope": "required",
      "hashes": [
        {
          "alg": "MD5",
          "content": "416757b9e6ba0563368ab59e668b3225"
        },
        {
          "alg": "SHA-1",
          "content": "88e3c593e9b3586e1c6177f89267da6fc6986f0c"
        },
        {
          "alg": "SHA-256",
          "content": "6ebd22ca1b9d8ec06d41de8d64e0596981d9607b42035f9ed374f9de271a481a"
        }
      ],
      "purl": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "https://brewweb.com/buildinfo?buildID=649279",
          "comment": "brew-build-id"
        }
      ],
      "type": "library",
      "bom-ref": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar"
    },
    {
      "group": "org.codehaus.mojo",
      "name": "animal-sniffer-annotations",
      "version": "1.17",
      "scope": "required",
      "hashes": [
        {
          "alg": "MD5",
          "content": "7ca108b790cf6ab5dbf5422cc79f0d89"
        },
        {
          "alg": "SHA-1",
          "content": "f97ce6decaea32b36101e37979f8b647f00681fb"
        },
        {
          "alg": "SHA-256",
          "content": "92654f493ecfec52082e76354f0ebf87648dc3d5cec2e3c3cdb947c016747a53"
        }
      ],
      "purl": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar",
      "externalReferences": [
        {
          "type": "build-system",
          "url": "https://brewweb.com/buildinfo?buildID=848654",
          "comment": "brew-build-id"
        }
      ],
      "type": "library",
      "bom-ref": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar"
    }
  ],
  "dependencies": [
    {
      "ref": "pkg:generic/my-broker-7.11.5.CR3-bin.zip@7.11.5.CR3?operation=OPBGCD23DVYAC",
      "dependsOn": [
        "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
        "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar"
      ]
    },
    {
      "ref": "pkg:maven/com.google.errorprone/error_prone_annotations@2.2.0?type=jar",
      "dependsOn": [

      ]
    },
    {
      "ref": "pkg:maven/org.codehaus.mojo/animal-sniffer-annotations@1.17?type=jar",
      "dependsOn": [

      ]
    }
  ]
}');
