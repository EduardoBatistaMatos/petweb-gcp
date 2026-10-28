# PETWEB — gestão de pets

Aplicação web para cadastrar e acompanhar pets, com histórico fotográfico por data. O frontend em React consome uma API Java; o PostgreSQL guarda os dados e metadados das fotos, e o Google Cloud Storage (GCS) armazena os arquivos das imagens.

## Funcionalidades

- Listagem de pets com foto principal quando disponível.
- Cadastro, consulta, edição e exclusão de pets.
- Envio de até 10 fotos por lote, associado a uma data.
- Histórico de fotos agrupado por data, com identificação da capa.
- Definição de outra foto como principal e exclusão individual de fotos.
- Documentação interativa da API com Swagger UI.

## Tecnologias e arquitetura

- **Frontend:** React 19, TypeScript, React Router e Vite.
- **Backend:** Java 21, `HttpServer` do JDK, JDBC e Jackson; não utiliza Spring ou Maven/Gradle.
- **Banco:** PostgreSQL, acessado exclusivamente pelo backend Java.
- **Imagens:** Google Cloud Storage, acessado exclusivamente pelo backend Java.
- **API:** especificação OpenAPI 3.0.3 em `backend/web/openapi.yaml` e Swagger UI local.

O frontend está em `frontend/src`. As rotas da interface são configuradas em `frontend/src/main.tsx`; as chamadas HTTP ficam em `frontend/src/services/petService.ts`. O backend está em `backend/src/main/java/br/com/petshop`, organizado em configuração, banco, modelos, repositórios, serviços e handlers HTTP.

## Requisitos

- JDK 21 ou superior (`java` e `javac` no `PATH`).
- PostgreSQL em execução e um banco acessível pela aplicação.
- Node.js e npm para executar o frontend.
- Um bucket GCS e credenciais com permissão para criar e excluir objetos, caso utilize upload e gerenciamento de fotos.

As dependências Java já estão em `backend/lib`; não é necessário instalar Maven ou Gradle. As dependências do frontend são declaradas em `frontend/package.json` e `frontend/package-lock.json`.

## Configuração

### Backend e PostgreSQL

Crie `backend/.env` a partir do modelo:

```powershell
Copy-Item backend/.env.example backend/.env
```

Configure os valores locais em `backend/.env`:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=petshop
DB_USER=postgres
DB_PASSWORD=sua_senha

SERVER_HOST=localhost
SERVER_PORT=8080

GCP_PROJECT_ID=seu-projeto-gcp
GCS_BUCKET_NAME=seu-bucket
GOOGLE_APPLICATION_CREDENTIALS=C:/caminho/para/sua-chave.json
```

`DB_PASSWORD` é necessária para iniciar a API. Variáveis de ambiente podem substituir as configurações do `.env`; `APP_ENV_FILE` permite indicar outro arquivo de configuração do banco. Para o GCS, o backend usa o caminho em `GOOGLE_APPLICATION_CREDENTIALS` quando configurado; sem ele, utiliza as credenciais padrão do ambiente. Nunca versione senhas ou chaves de conta de serviço.

### Estrutura do banco

O modelo de referência está em `backend/sql/schema.sql`. A tabela `pet` contém os dados do animal, o campo legado `imagem_url` e:

- `fotos` (`JSONB`): coleção de metadados das fotos, cada uma com identificador, `pet_id`, URL, `data_foto` e indicador `img_principal`.
- `img_principal` (`TEXT`): URL da foto de capa atual.

Os binários das imagens **não** são guardados no PostgreSQL. Uma única foto pode ser principal por pet; a API persiste sua URL em `img_principal` e deriva o indicador nas fotos retornadas. A coleção é ordenada pela data da foto, da mais recente para a mais antiga.

O backend não cria nem migra tabelas automaticamente. Para uma base que ainda não tenha as colunas do histórico, `backend/sql/migration_pet_fotos.sql` é uma migração **manual**. Faça backup e confira a estrutura e os dados existentes antes de aplicá-la: se `pet_foto` existir, o script copia seus metadados para `pet.fotos` e depois remove essa tabela. Não execute a migração sem validar esse efeito no banco alvo.

### Frontend

Por padrão, a interface chama `http://localhost:8080`. Para usar outro endereço, defina `VITE_API_BASE_URL` no ambiente de execução do Vite, por exemplo:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

## Executar localmente

Inicie primeiro o PostgreSQL, configure `backend/.env` e verifique que o banco e a estrutura estão prontos.

### Windows (PowerShell)

Terminal 1 — API:

```powershell
Set-Location backend
.\run.ps1
```

O script compila o backend e inicia a API em `http://localhost:8080` por padrão. Para compilar sem iniciar:

```powershell
.\build.ps1
```

Terminal 2 — frontend:

```powershell
Set-Location frontend
npm install
npm run dev
```

Abra o endereço local mostrado pelo Vite (normalmente `http://localhost:5173`).

### Linux/macOS

Terminal 1 — API:

```bash
cd backend
./build.sh
java -cp 'build/classes:lib/*' br.com.petshop.Main
```

Terminal 2 — frontend:

```bash
cd frontend
npm install
npm run dev
```

O projeto contém `backend/build.sh`; não há atualmente um `run.sh` no repositório.

## Rotas da interface

| Caminho | Tela |
|---|---|
| `/pets` | Lista de pets |
| `/pets/novo` | Cadastro em modal |
| `/pets/:id` | Dados do pet e histórico de fotos |
| `/pets/:id/editar` | Edição em modal |

## API HTTP

O endereço padrão da API é `http://localhost:8080`. As rotas disponíveis incluem:

| Método | Caminho | Descrição |
|---|---|---|
| `GET` | `/pets` | Lista pets |
| `POST` | `/pets` | Cadastra pet; aceita JSON ou formulário multipart com uma imagem legada opcional |
| `GET` | `/pets/{id}` | Consulta um pet |
| `PUT` | `/pets/{id}` | Atualiza os dados do pet |
| `DELETE` | `/pets/{id}` | Exclui o pet e seus arquivos associados |
| `GET` | `/pets/{id}/fotos` | Lista as fotos do pet, em ordem de data decrescente |
| `POST` | `/pets/{id}/fotos` | Envia fotos para o histórico |
| `PUT` | `/pets/{id}/fotos/{fotoId}/principal` | Define a capa do pet |
| `DELETE` | `/pets/{id}/fotos/{fotoId}` | Exclui uma foto e o objeto correspondente do GCS |
| `GET` | `/health` | Verifica a disponibilidade da API e do banco |

Para adicionar fotos, envie `multipart/form-data` para `/pets/{id}/fotos`, com uma data `data_foto` (`YYYY-MM-DD`) e de 1 a 10 arquivos no campo repetido `fotos`. O alias `foto` também é aceito. Cada imagem pode ter até 10 MiB; o corpo do lote pode ter até 100 MiB, além do overhead multipart. A primeira foto de um pet sem capa é definida como principal automaticamente. Ao excluir a capa, a foto mais recente restante é promovida; se não restarem fotos, o pet fica sem capa.

Cada objeto enviado ao GCS recebe um nome único. Na exclusão individual, o backend remove primeiro o objeto do bucket e, somente após a confirmação da remoção/ausência do objeto, atualiza os metadados no banco. O React não acessa o bucket diretamente.

### Contratos e documentação

- Swagger UI: <http://localhost:8080/docs/>
- OpenAPI YAML: <http://localhost:8080/openapi.yaml>
- Health check: <http://localhost:8080/health>

A raiz da API redireciona para `/docs/`. Os nomes dos campos JSON seguem `snake_case`; datas são transmitidas em ISO 8601. Os erros da API incluem `timestamp`, `status`, `error`, `message`, `path` e `details`.

## Comandos úteis

Na pasta `frontend`:

```bash
npm run dev      # servidor de desenvolvimento
npm run build    # verificação TypeScript e build de produção
npm run lint     # análise estática com Oxlint
npm run preview  # pré-visualização do build
```

Na pasta `backend`, `build.ps1` compila no Windows e `build.sh` compila em Linux/macOS. Não há atualmente um script de testes versionado na pasta do backend.

## Estrutura de pastas

```text
petweb-gcp/
|-- README.md
|-- backend/
|   |-- .env.example          # Modelo das variáveis de configuração
|   |-- build.ps1 / build.sh  # Compilação Java
|   |-- run.ps1               # Inicialização no Windows
|   |-- lib/                  # Dependências Java
|   |-- sql/                  # Schema e migração manual
|   |-- src/main/java/br/com/petshop/
|   |   |-- config/           # Configuração da aplicação
|   |   |-- db/               # Conexão com PostgreSQL
|   |   |-- model/            # Modelos e contratos de dados
|   |   |-- repository/       # Persistência
|   |   |-- service/          # Regras de negócio
|   |   |-- storage/          # Integração com armazenamento
|   |   `-- web/              # Handlers HTTP
|   `-- web/                  # OpenAPI e Swagger UI
`-- frontend/
    |-- package.json          # Dependências e comandos npm
    `-- src/                  # Interface React e integração com a API
```

## Preparar um banco novo

Com o PostgreSQL instalado e os comandos `createdb` e `psql` no `PATH`, execute a partir da raiz do projeto:

```bash
createdb -h localhost -p 5432 -U postgres petshop
psql -h localhost -p 5432 -U postgres -d petshop -v ON_ERROR_STOP=1 -f backend/sql/schema.sql
```

Os comandos podem solicitar a senha do usuário PostgreSQL. Ajuste host, porta, usuário e nome do banco conforme o seu ambiente e use os mesmos valores em `backend/.env`. Esse procedimento destina-se a uma base nova; para uma base existente, revise a estrutura antes de aplicar qualquer script. `CREATE TABLE IF NOT EXISTS` não acrescenta colunas a uma tabela já criada.

## Exemplos de uso da API

Os exemplos abaixo usam Bash e `curl`. No PowerShell, use `curl.exe` para evitar o alias de versões antigas e adapte as aspas e a continuação de linhas; o Swagger UI também permite executar essas requisições pelo navegador.

Consultar a saúde da aplicação e listar pets:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/pets
```

Cadastrar um pet com os campos obrigatórios (`nome`, `especie` e `responsavel`):

```bash
curl -X POST http://localhost:8080/pets \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Luna","especie":"Gato","responsavel":"Ana"}'
```

Utilize o identificador retornado pelo cadastro nas próximas requisições. O exemplo abaixo considera `id` igual a `1`:

```bash
curl http://localhost:8080/pets/1
curl -X POST http://localhost:8080/pets/1/fotos \
  -F 'data_foto=2026-10-28' \
  -F 'fotos=@/caminho/para/luna.jpg'
curl http://localhost:8080/pets/1/fotos
```

Repita o campo `-F 'fotos=@...'` para enviar várias imagens no mesmo lote, respeitando os limites descritos na seção de API. O upload depende da configuração do GCS.

## Verificação manual

Após iniciar os dois serviços:

1. Consulte `/health` e confirme a disponibilidade do banco.
2. Abra `/docs/` e verifique se a documentação da API carrega.
3. Na interface, cadastre um pet e confirme sua presença na listagem.
4. Edite os dados e confira se foram persistidos ao recarregar a página.
5. Com o GCS configurado, envie fotos, consulte o histórico e troque a capa.
6. Usando um pet de teste, exclua uma foto e depois o pet para conferir o fluxo de remoção.

Para verificar alterações de código, execute `npm run lint` e `npm run build` em `frontend`, além do script de compilação correspondente ao seu sistema em `backend`. A compilação não substitui a verificação dos fluxos que dependem de PostgreSQL e GCS.

## Solução de problemas

| Sintoma | O que verificar |
|---|---|
| `java` ou `javac` não encontrado | Instale o JDK 21 ou superior, confira o `PATH` e execute `java -version` e `javac -version`. |
| Falha de conexão com o banco | Confira se o PostgreSQL está ativo, se o banco existe e se host, porta, usuário e senha estão corretos. |
| Erro de tabela ou coluna inexistente | Compare a base com `backend/sql/schema.sql`; em bases antigas, revise a migração manual antes de aplicá-la. |
| Interface não consegue acessar a API | Confirme que o backend está ativo e confira `VITE_API_BASE_URL`; reinicie o Vite após alterar a configuração. |
| Falha no upload de fotos | Confira o bucket, as credenciais, as permissões da conta de serviço e os limites de quantidade e tamanho dos arquivos. |
| Porta já em uso | Libere a porta ou ajuste `SERVER_PORT`; se o endereço da API mudar, atualize também o frontend. |

## Cuidados com configuração e publicação

- Mantenha senhas, arquivos `.env` e chaves JSON fora dos commits. Use `backend/.env.example` como modelo sem credenciais reais.
- O build do frontend é gerado com `npm run build`, e `npm run preview` permite conferi-lo localmente.
- Variáveis `VITE_*` são incorporadas ao frontend e podem ser vistas pelo navegador; não armazene segredos nelas.
- Em uma hospedagem da interface com React Router, configure o servidor para servir `index.html` nas rotas da aplicação, permitindo abrir URLs como `/pets/1` diretamente.
- A configuração de credenciais do GCS pertence ao backend. A exibição das imagens exige que as URLs retornadas sejam acessíveis ao navegador conforme a política do bucket.
