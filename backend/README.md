# Petshop API

API RESTful para o CRUD de pets, implementada **em Java puro**, sem Spring Boot ou
outro framework de aplicação. O servidor HTTP é o `com.sun.net.httpserver.HttpServer`
do próprio JDK, o acesso ao banco usa **JDBC com `PreparedStatement`**, o JSON é
tratado com Jackson e a documentação interativa usa Swagger UI/OpenAPI.

## Tecnologias

- Java 21 (JDK)
- PostgreSQL 18 (compatível com versões anteriores que suportem a tabela existente)
- JDBC (`org.postgresql:postgresql`)
- Jackson para JSON
- Swagger UI + especificação OpenAPI 3.0.3
- `HttpServer` nativo do JDK para os endpoints HTTP

## Banco de dados

A estrutura existente foi verificada antes da implementação. O banco `petshop`
possui a tabela `public.pet` com a seguinte estrutura:

| Coluna              | Tipo                          | Nulo | Observação              |
|---------------------|-------------------------------|------|-------------------------|
| `id`                | `integer` / `serial`          | não  | Chave primária          |
| `nome`              | `varchar(100)`                | não  |                         |
| `especie`           | `varchar(50)`                 | não  |                         |
| `raca`              | `varchar(100)`                | sim  |                         |
| `porte`             | `varchar(30)`                 | sim  |                         |
| `responsavel`       | `varchar(150)`                | não  |                         |
| `data_nascimento`   | `date`                        | sim  |                         |
| `data_insercao_img` | `timestamp without time zone` | sim  |                         |
| `detalhes`          | `text`                        | sim  |                         |

A chave primária é `pet.id`, gerada pela sequência `pet_id_seq`. A API **não altera
a estrutura do banco**. O arquivo [`sql/schema.sql`](sql/schema.sql) serve apenas
como referência para recriar a tabela em outro ambiente.

## Estrutura do projeto

```text
backend/
├── .env                      # configuração local (não versionar)
├── .env.example              # modelo de configuração
├── .gitignore
├── build.ps1                 # compila no Windows/PowerShell
├── run.ps1                   # compila e inicia no Windows/PowerShell
├── build.sh / run.sh         # equivalentes para Linux/macOS
├── test.ps1                  # testes de integração de todos os endpoints
├── lib/                      # dependências .jar
├── sql/schema.sql            # referência da tabela (não é executado pela API)
├── web/
│   ├── openapi.yaml          # especificação OpenAPI
│   └── docs/                 # Swagger UI (HTML/CSS/JS locais)
└── src/main/java/br/com/petshop/
    ├── Main.java
    ├── config/AppConfig.java
    ├── db/Database.java
    ├── model/Pet.java
    ├── model/PetRequest.java
    ├── repository/PetRepository.java
    ├── service/PetService.java
    └── web/
        ├── ApiServer.java
        ├── handler/PetHandler.java
        ├── handler/HealthHandler.java
        ├── handler/DocsHandler.java
        ├── handler/OpenApiHandler.java
        ├── handler/RootHandler.java
        ├── http/ApiException.java
        ├── http/HttpUtils.java
        └── json/Json.java
```

## Requisitos para executar

1. **JDK 21 ou superior** instalado (`java` e `javac` no `PATH`).
2. **PostgreSQL** em execução.
3. Banco `petshop` e tabela `pet` existentes.
4. A pasta `lib/` já contém os JARs necessários; não é preciso Maven ou Gradle.

Verifique o Java:

```powershell
java -version
javac -version
```

## Configuração

As credenciais ficam no arquivo `.env`, na raiz do backend. O arquivo já está
configurado neste ambiente e não deve ser versionado (ele está no `.gitignore`).

Para outro ambiente, copie o modelo e preencha os valores:

```powershell
Copy-Item .env.example .env
```

Conteúdo esperado:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=petshop
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui
SERVER_HOST=localhost
SERVER_PORT=8080
```

Variáveis de ambiente com os mesmos nomes sobrescrevem os valores do `.env`.
Também é possível indicar outro arquivo com `APP_ENV_FILE`.

> A senha não está escrita em nenhuma classe Java. O `.env` é lido por
> `AppConfig`; se `DB_PASSWORD` não estiver definida, a API encerra com uma
> mensagem explicativa.

## Como executar

### Windows (PowerShell)

```powershell
.\build.ps1
.\run.ps1
```

O `run.ps1` executa o build e inicia a API. Para apenas compilar:

```powershell
.\build.ps1
```

### Linux/macOS

```bash
chmod +x build.sh run.sh
./run.sh
```

### Comandos manuais

Windows:

```powershell
javac --release 21 -encoding UTF-8 -cp "lib\*" -d build\classes (Get-ChildItem -Recurse -Filter *.java src\main\java).FullName
java -cp "build\classes;lib\*" br.com.petshop.Main
```

Linux/macOS:

```bash
javac --release 21 -encoding UTF-8 -cp "lib/*" -d build/classes $(find src/main/java -name '*.java')
java -cp "build/classes:lib/*" br.com.petshop.Main
```

Após iniciar, a API fica disponível em `http://localhost:8080`.

## Documentação Swagger/OpenAPI

Com a API em execução, abra no navegador:

- **Swagger UI:** <http://localhost:8080/docs/>
- **Especificação OpenAPI:** <http://localhost:8080/openapi.yaml>
- **Health check:** <http://localhost:8080/health>

A raiz `http://localhost:8080/` redireciona para o Swagger UI. Os arquivos do
Swagger UI são servidos localmente pela própria API, sem depender de CDN.

## Endpoints

| Método   | Rota          | Descrição                         | Sucesso | Erros principais        |
|----------|---------------|-----------------------------------|---------|-------------------------|
| `GET`    | `/pets`       | Lista todos os pets               | `200`   | `500`                   |
| `POST`   | `/pets`       | Cadastra um pet                   | `201`   | `400`, `413`, `415`, `500` |
| `GET`    | `/pets/{id}`  | Busca pet pelo identificador      | `200`   | `400`, `404`, `500`     |
| `PUT`    | `/pets/{id}`  | Atualiza todos os dados do pet    | `200`   | `400`, `404`, `413`, `415`, `500` |
| `DELETE` | `/pets/{id}`  | Exclui um pet                     | `204`   | `400`, `404`, `500`     |
| `GET`    | `/health`     | Verifica API e banco              | `200`   | `503`                   |
| `GET`    | `/pets/{id}/fotos` | Lista fotos por data (mais recentes primeiro) | `200` | `404`, `500` |
| `POST`   | `/pets/{id}/fotos` | Envia de 1 a 10 fotos multipart | `201` | `400`, `413`, `404`, `500` |
| `PUT`    | `/pets/{id}/fotos/{fotoId}/principal` | Define a capa | `200` | `404`, `500` |
| `DELETE` | `/pets/{id}/fotos/{fotoId}` | Exclui foto e objeto GCS | `204` | `400`, `404`, `500` |

O envio usa `multipart/form-data` com campos repetidos `fotos` e um único
`data_foto` (`YYYY-MM-DD`). Cada arquivo pode ter até 10 MiB e cada lote até
10 arquivos. A primeira imagem do primeiro lote é marcada como capa. Ao excluir
a capa, a foto mais recente remanescente é promovida automaticamente. Os objetos
são enviados e removidos pelo backend; o campo `url` guarda apenas a referência.

Antes de habilitar o histórico, aplique manualmente `sql/migration_pet_fotos.sql`
no banco alvo. A migração mantém uma única tabela (`pet`) e adiciona `fotos`
como JSONB, além de `img_principal` com a URL da capa. Se a tabela antiga
`pet_foto` existir, a migração copia seus metadados para `pet.fotos` antes de
removê-la. A aplicação não executa a migração.

O `POST` retorna o cabeçalho `Location` com a URL do recurso criado.
`DELETE` retorna `204` sem corpo.

### Corpo de criação/atualização

Campos obrigatórios: `nome`, `especie` e `responsavel`.
Campos opcionais: `raca`, `porte`, `data_nascimento`, `data_insercao_img` e
`detalhes`.

```json
{
  "nome": "Rex",
  "especie": "Cachorro",
  "raca": "Labrador",
  "porte": "Grande",
  "responsavel": "Maria Silva",
  "data_nascimento": "2020-05-10",
  "data_insercao_img": "2024-01-15T10:30:00",
  "detalhes": "Muito dócil e brincalhão."
}
```

Formatos aceitos:

- `data_nascimento`: `YYYY-MM-DD`
- `data_insercao_img`: `YYYY-MM-DDTHH:MM:SS` (data/hora local, sem fuso horário,
  compatível com `TIMESTAMP` sem time zone)

O `PUT` é uma substituição completa: os campos obrigatórios devem ser enviados
novamente; campos de texto opcionais enviados como `null` ou string vazia são
gravados como `NULL`.

### Exemplos com `curl`

Listar:

```bash
curl http://localhost:8080/pets
```

Cadastrar:

```bash
curl -i -X POST http://localhost:8080/pets \
  -H "Content-Type: application/json" \
  -d '{"nome":"Rex","especie":"Cachorro","responsavel":"Maria Silva","data_nascimento":"2020-05-10"}'
```

Buscar, atualizar e excluir:

```bash
curl http://localhost:8080/pets/1

curl -i -X PUT http://localhost:8080/pets/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"Rex","especie":"Cachorro","responsavel":"Maria Souza","porte":"Grande"}'

curl -i -X DELETE http://localhost:8080/pets/1
```

### Exemplos com PowerShell

```powershell
Invoke-RestMethod http://localhost:8080/pets

$corpo = @{
    nome = "Rex"
    especie = "Cachorro"
    responsavel = "Maria Silva"
    data_nascimento = "2020-05-10"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/pets `
    -ContentType "application/json" -Body $corpo
```

## Tratamento de erros

Os erros seguem o formato:

```json
{
  "timestamp": "2026-10-26T17:45:48.485748900-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos.",
  "path": "/pets",
  "details": [
    "O campo 'nome' é obrigatório.",
    "O campo 'responsavel' é obrigatório."
  ]
}
```

Códigos implementados:

- `200`: consulta/atualização bem-sucedida.
- `201`: pet cadastrado (com `Location`).
- `204`: exclusão bem-sucedida ou resposta a `OPTIONS`.
- `400`: JSON malformado, campo desconhecido, data inválida, validação ou ID inválido.
- `404`: pet ou rota não encontrado.
- `405`: método HTTP não permitido (com cabeçalho `Allow`).
- `413`: corpo maior que 1 MiB.
- `415`: `Content-Type` diferente de `application/json`.
- `500`: erro interno ou falha de conexão/consulta no PostgreSQL.
- `503`: banco indisponível no `/health`.

## Testes

O projeto inclui o script `test.ps1`, que compila a API, inicia um servidor de
teste na porta `18080`, executa as chamadas reais no PostgreSQL e encerra o
processo ao final. Ele testa:

- saúde da API e documentação (Swagger UI, CSS e OpenAPI);
- listagem, cadastro, busca, atualização e exclusão;
- validação de campos obrigatórios, datas, campos desconhecidos e JSON malformado;
- `Content-Type` inválido, corpo acima de 1 MiB, ID inválido e recurso inexistente;
- `405`, `OPTIONS`/CORS e `Location`;
- persistência real no banco e limpeza do registro criado.

Execute:

```powershell
.\test.ps1
```

Resultado da execução realizada neste ambiente:

```text
Resultado: 42 passaram, 0 falharam.
```

A especificação também foi validada com Redocly:

```text
Woohoo! Your API description is valid.
```

## Decisões de implementação

- **Sem framework de aplicação:** o roteamento e o ciclo HTTP usam `HttpServer`
  do JDK; não há Spring, Quarkus, Micronaut etc.
- **Separação de responsabilidades:** `config` lê a configuração, `db` fornece
  conexões, `repository` acessa a tabela, `service` valida regras de negócio,
  `handler` cuida de HTTP e `model` representa os dados.
- **SQL Injection:** todas as operações usam `PreparedStatement` com parâmetros.
- **Conexões:** uma conexão JDBC é aberta por operação e fechada com
  try-with-resources. Para este porte de projeto, evita complexidade de um pool;
  o driver usa timeout de conexão de 5 s e de socket de 30 s.
- **JSON:** ObjectMapper único, com `JavaTimeModule`, `snake_case`, rejeição de
  campos desconhecidos e de tokens extras. Datas são serializadas em ISO 8601.
- **Validação:** campos obrigatórios, tamanhos máximos iguais aos do banco,
  normalização de espaços e verificação de ID positivo.
- **CORS:** respostas incluem `Access-Control-Allow-Origin: *` e `OPTIONS` é
  tratado, facilitando o consumo pelo frontend local.
- **Documentação:** OpenAPI mantido manualmente em `web/openapi.yaml` e Swagger UI
  servido pela própria API.

## Dependências

Os JARs estão em `lib/`:

| Dependência                    | Versão   | Finalidade                          |
|--------------------------------|----------|-------------------------------------|
| `org.postgresql:postgresql`    | 42.7.14  | Driver JDBC do PostgreSQL           |
| `jackson-databind`             | 2.22.4   | Serialização/desserialização JSON   |
| `jackson-core`                 | 2.22.4   | Núcleo do Jackson                   |
| `jackson-annotations`          | 2.22     | Anotações do Jackson                |
| `jackson-datatype-jsr310`      | 2.22.4   | Suporte a `LocalDate`/`LocalDateTime` |
| Swagger UI                     | 5.34.0   | Interface de documentação e testes  |

## Solução de problemas

- **`DB_PASSWORD não configurada`**: crie o `.env` a partir do `.env.example`
  ou defina `DB_PASSWORD` no ambiente.
- **Falha de autenticação no PostgreSQL**: confira usuário, senha, porta e se o
  serviço do PostgreSQL está em execução.
- **`Address already in use`**: altere `SERVER_PORT` no `.env` ou encerre o
  processo que está usando a porta (por exemplo, `8080`).
- **`javac`/`java` não encontrado**: instale o JDK 21+ e adicione a pasta `bin`
  ao `PATH`.
- **Execução de scripts bloqueada no PowerShell**: rode
  `Set-ExecutionPolicy -Scope Process Bypass` na sessão atual e execute novamente.
- **Swagger UI abre, mas a especificação não carrega**: confirme que
  `web/openapi.yaml` existe e que a API foi iniciada a partir da pasta
  `backend` (os scripts já fazem isso).
- **Banco indisponível**: consulte `GET /health`; o retorno `503` indica que a
  API está no ar, mas não consegue acessar o PostgreSQL.

## Observações

- A API não implementa autenticação/autorização, pois isso não foi solicitado.
- A tabela `pet` não foi criada nem alterada pela aplicação; apenas foi
  consultada e utilizada pelo CRUD.
- O arquivo `.env` contém credenciais locais e está no `.gitignore`.
- Para recriar a tabela em outro ambiente, use `sql/schema.sql` com autorização.
