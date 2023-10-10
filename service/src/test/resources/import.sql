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
INSERT INTO sbom_generation_request(
		id,
		creation_time,
		build_id,
		status,
		result,
		config
	)
VALUES (
		'AASSBB',
		now(),
		'ARYT3LBXDVYAC',
		'FINISHED',
		'SUCCESS',
		'{
"buildId": "ARYT3LBXDVYAC",
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
"apiVersion": "sbomer.jboss.org/v1alpha1"
}
		
		'
	);
INSERT INTO sbom(
		id,
		root_purl,
		creation_time,
		build_id,
		generationRequest_id
	)
VALUES (
		'416640206274228224',
		'pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom',
		now(),
		'ARYT3LBXDVYAC',
		'AASSBB'
	);