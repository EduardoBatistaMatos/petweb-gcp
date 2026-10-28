$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "javac não encontrado no PATH. Instale o JDK 21 ou superior."
}

$fontes = @(Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse |
    Select-Object -ExpandProperty FullName)

if ($fontes.Count -eq 0) {
    throw "Nenhum arquivo .java encontrado em src\main\java."
}

if (Test-Path "build\classes") {
    Remove-Item -Recurse -Force "build\classes"
}
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

Write-Host "Compilando $($fontes.Count) arquivo(s) Java..."
& javac --release 21 -encoding UTF-8 -cp "lib\*" -d "build\classes" $fontes

if ($LASTEXITCODE -ne 0) {
    throw "Falha na compilação."
}

Write-Host "Build concluído em build\classes."
