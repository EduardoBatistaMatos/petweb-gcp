$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "java não encontrado no PATH. Instale o JDK 21 ou superior."
}

& "$PSScriptRoot\build.ps1"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Iniciando a API..."
& java -cp "build\classes;lib\*" br.com.petshop.Main

exit $LASTEXITCODE
