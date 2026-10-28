#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  echo "javac não encontrado no PATH. Instale o JDK 21 ou superior." >&2
  exit 1
fi

rm -rf build/classes
mkdir -p build/classes

mapfile -t fontes < <(find src/main/java -name '*.java' -print)
if [ "${#fontes[@]}" -eq 0 ]; then
  echo "Nenhum arquivo .java encontrado em src/main/java." >&2
  exit 1
fi

echo "Compilando ${#fontes[@]} arquivo(s) Java..."
javac --release 21 -encoding UTF-8 -cp "lib/*" -d build/classes "${fontes[@]}"
echo "Build concluído em build/classes."
