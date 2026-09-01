#!/usr/bin/env bash
# Uso: ./run.sh [caminho/para/model.litertlm]
# Sem argumento, usa models/gemma3-1b-it-int4.litertlm.
set -euo pipefail
cd "$(dirname "$0")"
MODEL="${1:-$PWD/models/gemma3-1b-it-int4.litertlm}"
if [[ ! -f "$MODEL" ]]; then
  echo "Modelo nao encontrado: $MODEL" >&2
  echo "Baixe um .litertlm de https://huggingface.co/litert-community" >&2
  exit 1
fi
exec ./gradlew run -q --console=plain --args="$MODEL"
