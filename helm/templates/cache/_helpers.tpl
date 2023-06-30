#
# JBoss, Home of Professional Open Source.
# Copyright 2023 Red Hat, Inc., and individual contributors
# as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

{{/*
Create the name of the service account to use
*/}}
{{- define "sbomer.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default ( .Release.Name ) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "sbomer-sa" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Default labels
*/}}
{{- define "sbomer.labels" -}}
{{- $component := "" }}
{{- if ge (len .) 2 }}{{ $component = index . 1 }}{{ end }}
{{- with index . 0 -}}

labels:
{{- if $component }}
  app.kubernetes.io/component: {{ $component }}
  app.kubernetes.io/name: {{ .Release.Name }}-{{ $component }}
{{- else }}
  app.kubernetes.io/name: {{ .Release.Name }}
{{- end }}
  app.kubernetes.io/instance: {{ .Release.Name }}
  app.kubernetes.io/managed-by: {{ .Release.Service }}
  app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end -}}

{{/*
Default selector
*/}}
{{- define "sbomer.selector" -}}
{{- $component := "" }}
{{- if ge (len .) 2 }}{{ $component = index . 1 }}{{ end }}
{{- with index . 0 -}}

{{- if $component -}}
app.kubernetes.io/component: {{ $component }}
app.kubernetes.io/name: {{ .Release.Name }}-{{ $component }}
{{- else }}
app.kubernetes.io/name: {{ .Release.Name }}
{{- end }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
{{- end -}}