param(
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if ($null -eq ("System.Net.Http.HttpClient" -as [type])) {
    Add-Type -AssemblyName System.Net.Http
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "java não encontrado no PATH. Instale o JDK 21 ou superior."
}

& "$PSScriptRoot\build.ps1"

$baseUrl = "http://127.0.0.1:$Port"
$saida = Join-Path $PSScriptRoot "build\test-server.out.log"
$erro = Join-Path $PSScriptRoot "build\test-server.err.log"
Remove-Item $saida, $erro -ErrorAction SilentlyContinue

$portaAnterior = $env:SERVER_PORT
$hostAnterior = $env:SERVER_HOST
$env:SERVER_PORT = "$Port"
$env:SERVER_HOST = "127.0.0.1"

$script:Passou = 0
$script:Falhou = 0

function Assert-True {
    param(
        [string]$Nome,
        [bool]$Condicao,
        [string]$Detalhe = ""
    )
    if ($Condicao) {
        $script:Passou++
        Write-Host "[PASS] $Nome" -ForegroundColor Green
    } else {
        $script:Falhou++
        if ($Detalhe) {
            Write-Host "[FAIL] $Nome -> $Detalhe" -ForegroundColor Red
        } else {
            Write-Host "[FAIL] $Nome" -ForegroundColor Red
        }
    }
}

function Assert-Igual {
    param(
        [string]$Nome,
        $Esperado,
        $Atual
    )
    Assert-True $Nome ($Esperado -eq $Atual) "esperado=$Esperado, obtido=$Atual"
}

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseProxy = $false
$cliente = [System.Net.Http.HttpClient]::new($handler)
$cliente.Timeout = [TimeSpan]::FromSeconds(10)

function Invoke-Api {
    param(
        [string]$Metodo,
        [string]$Caminho,
        [string]$Corpo = $null,
        [string]$ContentType = "application/json"
    )

    $requisicao = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Metodo),
        "$baseUrl$Caminho"
    )
    if ($Metodo -in @("POST", "PUT", "PATCH")) {
        $requisicao.Content = [System.Net.Http.StringContent]::new(
            $Corpo,
            [System.Text.Encoding]::UTF8,
            $ContentType
        )
    }

    $resposta = $cliente.SendAsync($requisicao).GetAwaiter().GetResult()
    $texto = $resposta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $json = $null
    if ($texto) {
        try {
            $json = $texto | ConvertFrom-Json
        } catch {
            $json = $null
        }
    }

    $valoresAllow = $null
    $allow = @()
    $temAllow = $resposta.Headers.TryGetValues("Allow", [ref]$valoresAllow)
    if (-not $temAllow -and $null -ne $resposta.Content) {
        $temAllow = $resposta.Content.Headers.TryGetValues("Allow", [ref]$valoresAllow)
    }
    if ($temAllow) {
        $allow = @($valoresAllow | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() })
    }

    [pscustomobject]@{
        Status = [int]$resposta.StatusCode
        Corpo = $json
        Texto = $texto
        Headers = $resposta.Headers
        Allow = $allow
    }
}

$processo = $null
$idCriado = $null
$clienteSaude = $null
$handlerSaude = $null

try {
    Write-Host "Iniciando servidor de teste na porta $Port..."
    $processo = Start-Process `
        -FilePath "java" `
        -ArgumentList @("-cp", "build\classes;lib\*", "br.com.petshop.Main") `
        -WorkingDirectory $PSScriptRoot `
        -RedirectStandardOutput $saida `
        -RedirectStandardError $erro `
        -PassThru

    $handlerSaude = [System.Net.Http.HttpClientHandler]::new()
    $handlerSaude.UseProxy = $false
    $clienteSaude = [System.Net.Http.HttpClient]::new($handlerSaude)
    $clienteSaude.Timeout = [TimeSpan]::FromSeconds(2)
    $disponivel = $false
    for ($i = 0; $i -lt 60; $i++) {
        if ($processo.HasExited) {
            break
        }
        try {
            $respostaSaude = $clienteSaude.GetAsync("$baseUrl/health").GetAwaiter().GetResult()
            if ([int]$respostaSaude.StatusCode -eq 200) {
                $disponivel = $true
                break
            }
        } catch {
            # O servidor ainda pode estar iniciando.
        }
        Start-Sleep -Milliseconds 250
    }

    if (-not $disponivel) {
        Write-Host "--- stderr do servidor ---" -ForegroundColor Yellow
        Get-Content $erro -ErrorAction SilentlyContinue
        Write-Host "--- stdout do servidor ---" -ForegroundColor Yellow
        Get-Content $saida -ErrorAction SilentlyContinue
        throw "O servidor não ficou disponível em $baseUrl/health."
    }

    Write-Host "`nExecutando testes...`n" -ForegroundColor Cyan

    # Saúde e documentação
    $health = Invoke-Api "GET" "/health"
    Assert-Igual "GET /health retorna 200" 200 $health.Status
    Assert-Igual "GET /health informa banco ativo" "up" $health.Corpo.database

    $docs = Invoke-Api "GET" "/docs/"
    Assert-Igual "GET /docs/ retorna 200" 200 $docs.Status
    Assert-True "GET /docs/ entrega o Swagger UI" ($docs.Texto -match "swagger-ui-bundle\.js")

    $css = Invoke-Api "GET" "/docs/swagger-ui.css"
    Assert-Igual "GET /docs/swagger-ui.css retorna 200" 200 $css.Status

    $openapi = Invoke-Api "GET" "/openapi.yaml"
    Assert-Igual "GET /openapi.yaml retorna 200" 200 $openapi.Status
    Assert-True "OpenAPI contém o path /pets" ($openapi.Texto -match "/pets")

    $semRedirecionar = [System.Net.Http.HttpClientHandler]::new()
    $semRedirecionar.AllowAutoRedirect = $false
    $semRedirecionar.UseProxy = $false
    $clienteRedirect = [System.Net.Http.HttpClient]::new($semRedirecionar)
    $respostaRaiz = $clienteRedirect.GetAsync("$baseUrl/").GetAwaiter().GetResult()
    Assert-Igual "GET / redireciona" 302 ([int]$respostaRaiz.StatusCode)
    Assert-True "GET / aponta para /docs/" ($respostaRaiz.Headers.Location.ToString() -match "/docs/$")
    $clienteRedirect.Dispose()
    $semRedirecionar.Dispose()

    # Listagem inicial
    $listaInicial = Invoke-Api "GET" "/pets"
    Assert-Igual "GET /pets retorna 200" 200 $listaInicial.Status
    Assert-True "GET /pets retorna uma lista JSON" ($listaInicial.Texto.TrimStart().StartsWith("["))

    # Cadastro
    $nomeTeste = "Teste API " + [Guid]::NewGuid().ToString("N").Substring(0, 8)
    $corpoCriacao = @{
        nome = $nomeTeste
        especie = "Cachorro"
        raca = "Vira-lata"
        porte = "Médio"
        responsavel = "Teste Automatizado"
        data_nascimento = "2020-05-10"
        data_insercao_img = "2024-01-15T10:30:00"
        detalhes = "Criado pelo test.ps1"
    } | ConvertTo-Json -Compress

    $criado = Invoke-Api "POST" "/pets" $corpoCriacao
    Assert-Igual "POST /pets retorna 201" 201 $criado.Status
    Assert-True "POST /pets retorna id" ($null -ne $criado.Corpo.id)
    Assert-True "POST /pets retorna Location" ($criado.Headers.Location.ToString() -match "^/pets/\d+$")
    Assert-Igual "POST /pets grava o nome" $nomeTeste $criado.Corpo.nome
    Assert-Igual "POST /pets grava a data de nascimento" "2020-05-10" $criado.Corpo.data_nascimento
    Assert-Igual "POST /pets grava a data da imagem" "2024-01-15T10:30:00" $criado.Corpo.data_insercao_img
    $idCriado = [int]$criado.Corpo.id

    # Consulta por id
    $porId = Invoke-Api "GET" "/pets/$idCriado"
    Assert-Igual "GET /pets/{id} retorna 200" 200 $porId.Status
    Assert-Igual "GET /pets/{id} retorna o pet correto" $nomeTeste $porId.Corpo.nome

    # Listagem com o registro criado
    $lista = Invoke-Api "GET" "/pets"
    Assert-Igual "GET /pets continua retornando 200" 200 $lista.Status
    $encontrado = @($lista.Corpo | Where-Object { $_.id -eq $idCriado }).Count
    Assert-True "GET /pets contém o pet criado" ($encontrado -ge 1)

    # Atualização
    $corpoAtualizacao = @{
        nome = "$nomeTeste atualizado"
        especie = "Cachorro"
        raca = "Labrador"
        porte = "Grande"
        responsavel = "Teste Automatizado 2"
        data_nascimento = "2021-01-01"
        data_insercao_img = $null
        detalhes = "Atualizado pelo test.ps1"
    } | ConvertTo-Json -Compress

    $atualizado = Invoke-Api "PUT" "/pets/$idCriado" $corpoAtualizacao
    Assert-Igual "PUT /pets/{id} retorna 200" 200 $atualizado.Status
    Assert-Igual "PUT /pets/{id} atualiza o nome" "$nomeTeste atualizado" $atualizado.Corpo.nome
    Assert-Igual "PUT /pets/{id} atualiza a raça" "Labrador" $atualizado.Corpo.raca
    Assert-True "PUT /pets/{id} aceita data de imagem nula" ($null -eq $atualizado.Corpo.data_insercao_img)

    # Erros de validação e requisição
    $semObrigatorios = Invoke-Api "POST" "/pets" '{"especie":"Gato"}'
    Assert-Igual "POST /pets sem campos obrigatórios retorna 400" 400 $semObrigatorios.Status
    Assert-True "Erro de validação cita o campo nome" ($semObrigatorios.Texto -match "nome")

    $dataInvalida = Invoke-Api "POST" "/pets" '{"nome":"X","especie":"Gato","responsavel":"Y","data_nascimento":"10/05/2020"}'
    Assert-Igual "POST /pets com data inválida retorna 400" 400 $dataInvalida.Status

    $campoDesconhecido = Invoke-Api "POST" "/pets" '{"nome":"X","especie":"Gato","responsavel":"Y","foo":"bar"}'
    Assert-Igual "POST /pets com campo desconhecido retorna 400" 400 $campoDesconhecido.Status

    $jsonMalformado = Invoke-Api "POST" "/pets" '{"nome":'
    Assert-Igual "POST /pets com JSON malformado retorna 400" 400 $jsonMalformado.Status

    $tipoInvalido = Invoke-Api "POST" "/pets" '{}' "text/plain"
    Assert-Igual "POST /pets com Content-Type inválido retorna 415" 415 $tipoInvalido.Status

    $corpoGrande = '{"nome":"X","especie":"Gato","responsavel":"Y","detalhes":"' + ('a' * 1048600) + '"}'
    $muitoGrande = Invoke-Api "POST" "/pets" $corpoGrande
    Assert-Igual "POST /pets com corpo acima de 1 MiB retorna 413" 413 $muitoGrande.Status

    $idInvalido = Invoke-Api "GET" "/pets/abc"
    Assert-Igual "GET /pets/abc retorna 400" 400 $idInvalido.Status

    $inexistente = Invoke-Api "GET" "/pets/999999"
    Assert-Igual "GET /pets/999999 retorna 404" 404 $inexistente.Status

    $atualizarInexistente = Invoke-Api "PUT" "/pets/999999" $corpoAtualizacao
    Assert-Igual "PUT /pets/999999 retorna 404" 404 $atualizarInexistente.Status

    $excluirInexistente = Invoke-Api "DELETE" "/pets/999999"
    Assert-Igual "DELETE /pets/999999 retorna 404" 404 $excluirInexistente.Status

    $metodoInvalido = Invoke-Api "PATCH" "/pets"
    Assert-Igual "PATCH /pets retorna 405" 405 $metodoInvalido.Status
    Assert-True "405 informa os métodos permitidos" ($metodoInvalido.Allow -contains "GET")

    $options = Invoke-Api "OPTIONS" "/pets"
    Assert-Igual "OPTIONS /pets retorna 204" 204 $options.Status
    Assert-True "OPTIONS informa CORS" ($options.Headers.Contains("Access-Control-Allow-Origin"))

    # Exclusão
    $excluido = Invoke-Api "DELETE" "/pets/$idCriado"
    Assert-Igual "DELETE /pets/{id} retorna 204" 204 $excluido.Status
    $idCriado = $null

    $depoisDeExcluir = Invoke-Api "GET" "/pets/$($porId.Corpo.id)"
    Assert-Igual "GET /pets/{id} após exclusão retorna 404" 404 $depoisDeExcluir.Status
} finally {
    if ($null -ne $idCriado) {
        try {
            Invoke-Api "DELETE" "/pets/$idCriado" | Out-Null
            Write-Host "Registro de teste removido." -ForegroundColor DarkGray
        } catch {
            Write-Host "Não foi possível remover o registro de teste $idCriado." -ForegroundColor Yellow
        }
    }

    if ($null -ne $processo -and -not $processo.HasExited) {
        Stop-Process -Id $processo.Id -Force
        $processo.WaitForExit(5000) | Out-Null
    }

    if ($null -ne $clienteSaude) {
        $clienteSaude.Dispose()
    }
    if ($null -ne $handlerSaude) {
        $handlerSaude.Dispose()
    }

    if ($null -eq $portaAnterior) {
        Remove-Item Env:SERVER_PORT -ErrorAction SilentlyContinue
    } else {
        $env:SERVER_PORT = $portaAnterior
    }
    if ($null -eq $hostAnterior) {
        Remove-Item Env:SERVER_HOST -ErrorAction SilentlyContinue
    } else {
        $env:SERVER_HOST = $hostAnterior
    }

    $cliente.Dispose()
}

Write-Host ""
Write-Host "Resultado: $script:Passou passaram, $script:Falhou falharam." -ForegroundColor Cyan
if ($script:Falhou -gt 0) {
    exit 1
}
exit 0
