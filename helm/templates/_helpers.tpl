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