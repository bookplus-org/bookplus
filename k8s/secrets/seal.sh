#!/usr/bin/env bash
# Sella secrets.example.yaml (con valores REALES ya rellenados) en sealed-secrets.yaml,
# que SÍ es seguro commitear al repo (solo el controlador del clúster puede descifrarlo).
#
# Requisitos: kubeseal instalado y el controlador Sealed Secrets desplegado en el clúster.
set -euo pipefail
IN="${1:-secrets.example.yaml}"
OUT="${2:-sealed-secrets.yaml}"

: > "$OUT"
# kubeseal procesa un Secret por documento
csplit -z -f /tmp/sec_ -b '%02d.yaml' "$IN" '/^---$/' '{*}' >/dev/null 2>&1 || true
for f in /tmp/sec_*.yaml; do
  [ -s "$f" ] || continue
  grep -q 'kind: Secret' "$f" || continue
  kubeseal --format yaml < "$f" >> "$OUT"
  echo "---" >> "$OUT"
done
rm -f /tmp/sec_*.yaml
echo "OK -> $OUT (seguro para commitear)"
