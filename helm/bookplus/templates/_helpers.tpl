{{- define "bookplus.labels" -}}
app.kubernetes.io/part-of: bookplus
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: bookplus-{{ .Chart.Version }}
{{- end -}}
