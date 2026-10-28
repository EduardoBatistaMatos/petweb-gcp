#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "java não encontrado no PATH. Instale o JDK 21 ou superior." >&2
  exit 1
fi

./build.sh
echo "Iniciando a API..."
exec java -cp "build/classes:lib/*" br.com.petshop.Main
