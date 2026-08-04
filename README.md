# Spring AI Budgeting Assistant

API financeira segura, com dois jeitos de registrar e consultar gastos: endpoints REST
tradicionais e um assistente de voz construído com Spring AI e OpenAI. O projeto existe para
praticar, em conjunto, Spring Boot, Spring AI, Test-Driven Development, AppSec e DevSecOps num
cenário concreto: uma IA que pode *sugerir* uma ação financeira, mas nunca *autorizar* sozinha
nada - quem decide é código Java determinístico.

## Status

Implementação inicial completa: domínio, casos de uso, persistência PostgreSQL/Flyway, API REST,
autenticação, upload de áudio validado, orquestração de voz com Tool Calling allowlisted,
documentação de segurança e pipeline DevSecOps.

## Desafio original

Baseado no desafio final da trilha Spring Boot da DIO:

- <https://github.com/digitalinnovationone/dio-spring-boot-learning-track>
- <https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai>

Este projeto **é** esse desafio, não um projeto diferente. O núcleo funcional pedido pelo desafio
está todo aqui e continua reconhecível: uma aplicação Spring Boot recebe áudio, transcreve com
Spring AI usando OpenAI, interpreta a intenção com `ChatClient`, executa um caso de uso de
transação através de Tool Calling, persiste/consulta em um banco relacional, e devolve a resposta
final como áudio MP3. Tudo descrito nas seções de AppSec, TDD e DevSecOps abaixo é reforço em
volta desse fluxo central, não substituição dele.

## Aderência ao desafio

| Requisito do desafio | Implementação |
|---|---|
| Upload de áudio | `AssistantController` (`POST /api/v1/assistant/audio`) |
| Conversão de fala em texto | `TranscriptionModel` do Spring AI (OpenAI speech-to-text) |
| Interpretação da intenção | `ChatClient` (`VoiceAssistantOrchestrationService`) |
| Execução da ação na aplicação | `TransactionTools` (Tool Calling allowlisted) |
| Persistência da transação | `PersistTransactionUseCase` |
| Consulta por categoria | `ListTransactionsByCategoryUseCase` |
| Persistência relacional | Adapter PostgreSQL (`TransactionRepositoryAdapter` + Flyway) |
| Conversão da resposta em voz | `TextToSpeechModel` do Spring AI (OpenAI text-to-speech), retornado como `audio/mpeg` |

## Evoluções implementadas

Em cima do desafio original, este projeto acrescenta:

- **PostgreSQL no lugar do MySQL** do projeto de referência da DIO — troca de motor relacional,
  não de paradigma: o contrato de persistência relacional (tabela `transactions`, chave primária,
  consultas por categoria) é o mesmo que o desafio pedia; PostgreSQL foi a escolha porque é o banco
  exigido para este exercício, com Flyway controlando o schema e Testcontainers testando contra
  uma instância real, não simulada.
- **Test-Driven Development** em toda fatia significativa (domínio, casos de uso, persistência,
  REST, IA) — ver [`docs/security/security-testing.md`](docs/security/security-testing.md) e o
  relatório de implementação para os ciclos RED → GREEN documentados.
- **Flyway** como fonte única de verdade do schema (`spring.jpa.hibernate.ddl-auto=validate`).
- **Testcontainers** para os testes de persistência contra PostgreSQL real, sem H2.
- **Autenticação HTTP Basic** obrigatória em todo endpoint financeiro.
- **Validação de upload de áudio** (tamanho, extensão, media type) antes de qualquer chamada à
  OpenAI.
- **Proteção de segredos** (`OPENAI_API_KEY`, credenciais de banco e de aplicação sempre via
  ambiente, nunca no código).
- **Mitigações de prompt injection** (allowlist de ferramentas, contexto de confiança do servidor,
  idempotência) — ver [`docs/security/prompt-injection.md`](docs/security/prompt-injection.md).
- **Pipeline DevSecOps** (Gitleaks, CodeQL, revisão de dependências, Trivy, SBOM, Dependabot).

Nenhum desses itens muda o que a aplicação faz do ponto de vista do desafio - eles protegem e
comprovam o mesmo fluxo.

## Arquitetura

```text
dev.tetel.budgeting
├── domain          // Transaction, TransactionId, Category, TransactionSource, Money,
│                    // TransactionRepository (interface). Zero dependência de Spring/JPA/HTTP/IA.
├── application      // PersistTransactionUseCase, ListTransactionsByCategoryUseCase, DTOs.
│                    // Depende só do domínio; testável sem Spring, Docker ou rede.
└── infrastructure
    ├── persistence  // TransactionEntity (JPA), TransactionRepositoryAdapter, mapeamento explícito
    ├── security     // SecurityConfig (HTTP Basic, stateless, CSRF documentado)
    ├── web          // TransactionController, AssistantController, GlobalExceptionHandler
    ├── audio        // AudioUploadValidator (validação local, antes de qualquer chamada à OpenAI)
    └── ai           // TransactionTools (allowlist), VoiceAssistantOrchestrationService, AiConfig
```

Cada classe existe para responder quatro perguntas: por que ela existe, quem a chama, qual regra
ou fronteira ela garante, e qual teste prova esse comportamento. Não há abstrações criadas "porque
sim" - por exemplo, `TransactionOutput` é reaproveitado como corpo de resposta HTTP porque já é um
DTO seguro; não existe uma segunda classe de resposta REST só para existir.

### Fluxo REST tradicional (sem IA)

```text
POST /api/v1/transactions
  → CreateTransactionRequest (validado com Bean Validation)
  → TransactionController deriva source=REST, ownerUsername (do principal autenticado) e
    requestId (gerado pelo servidor) - nada disso vem do corpo da requisição
  → PersistTransactionUseCase → Transaction.create(...) (valida invariantes de domínio)
  → TransactionRepository → PostgreSQL
  → TransactionOutput (DTO seguro, nunca a entidade JPA)
```

### Fluxo do assistente de voz

```text
Upload de áudio
  → AudioUploadValidator (tamanho, extensão, media type - tudo antes de qualquer chamada à OpenAI)
  → OpenAI speech-to-text (Spring AI TranscriptionModel)
  → ChatClient interpreta a intenção (a transcrição entra só como conteúdo de usuário,
    nunca como instrução de sistema)
  → Tool Calling restrito a TransactionTools (persistTransaction, listTransactionsByCategory)
    - ownerUsername e requestId vêm de ToolContext, preenchido pelo servidor, nunca pelo modelo
  → mesmos casos de uso da aplicação usados pelo fluxo REST
  → PostgreSQL
  → resposta textual segura
  → OpenAI text-to-speech (Spring AI TextToSpeechModel)
  → MP3 (audio/mpeg)
```

Ver [`docs/security/prompt-injection.md`](docs/security/prompt-injection.md) para o detalhamento
completo de por que esse desenho resiste a instruções maliciosas embutidas no áudio.

## Tecnologias e versões

Nenhuma versão aqui foi escolhida de memória - todas foram verificadas via Spring Initializr,
documentação oficial e inspeção real dos artefatos baixados, porque o par Spring Boot
4/Spring AI 2 é recente o suficiente para ter mudanças de API relevantes desde a última vez que
essas ferramentas foram amplamente documentadas.

| Componente | Versão | Observação |
|---|---|---|
| Java | 21 (toolchain) | Baseline mínima do Spring Boot 4 |
| Gradle Wrapper | 9.5.1 | Gerado pelo Spring Initializr para Boot 4.1 |
| Spring Boot | 4.1.0 | Linha atualmente suportada; **Spring Boot 3.5 chegou ao fim de vida (OSS) em 30/06/2026**, então não é uma escolha defensável para um projeto novo |
| Spring AI | 2.0.0 (BOM) | Depende obrigatoriamente do Spring Boot 4.x |
| PostgreSQL | 18 (`postgres:18-alpine`) | Único banco de dados usado no projeto |
| Testcontainers | 2.0.5 (gerenciado pelo BOM do Boot) | Módulos renomeados na v2: `org.testcontainers:testcontainers-postgresql`, pacote `org.testcontainers.postgresql.PostgreSQLContainer` |
| Flyway | 12.4.0 (gerenciado pelo BOM do Boot) | Fonte única de verdade do schema |
| Driver JDBC PostgreSQL | 42.7.13 (override explícito) | Boot 4.1.0 gerencia 42.7.11; subimos a versão para corrigir a CVE-2026-54291 (ver `docs/security/security-controls.md`) |
| Netty | 4.2.16.Final (BOM `io.netty:netty-bom` importado após o BOM do Spring AI) | Boot 4.1.0 gerencia 4.2.15.Final; nunca é dependência direta - chega via `spring-ai-starter-model-openai` (cliente HTTP do SDK da OpenAI); subimos para corrigir 6 CVEs HIGH |

Decisões de compatibilidade que valem registrar:

- **Spring Boot 4 muda o padrão de CSRF do Spring Security 7** (agora habilitado por padrão) -
  este projeto desabilita CSRF explicitamente e documenta o porquê em
  [`docs/security/security-controls.md`](docs/security/security-controls.md).
- **Testcontainers 2.0 renomeia módulos e pacotes** em relação à série 1.x (ex.:
  `org.testcontainers:postgresql` virou `org.testcontainers:testcontainers-postgresql`) - o
  `build.gradle` já usa os nomes corretos para esta combinação de versões.
- **`HttpStatus.PAYLOAD_TOO_LARGE` foi renomeado para `HttpStatus.CONTENT_TOO_LARGE`** no Spring
  Framework 7 - usado assim no `GlobalExceptionHandler`.

## Pré-requisitos

- WSL2 + Ubuntu 24.04 (ou Linux equivalente)
- Java 21
- Docker Engine + Docker Compose
- GitHub CLI (`gh`)
- Git configurado com SSH

Não é necessária instalação global do Gradle - use sempre o wrapper (`./gradlew`).

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `OPENAI_API_KEY` | Autenticação com a API da OpenAI (Spring AI) |
| `POSTGRES_DB` | Nome do banco (`budgeting`) |
| `POSTGRES_USER` | Usuário dedicado da aplicação (nunca o superusuário do Postgres - ver abaixo) |
| `POSTGRES_PASSWORD` | Senha do usuário da aplicação |
| `APP_SECURITY_USERNAME` | Usuário do HTTP Basic da API |
| `APP_SECURITY_PASSWORD` | Senha do HTTP Basic da API |

## Configuração segura de segredos

```bash
cp .env.example .env
# edite .env com valores locais - nunca um segredo real de produção
```

`.env` está no `.gitignore`; `.env.example` contém só placeholders e é o único dos dois que fica
versionado. A build padrão e a suíte de testes padrão **não** precisam de `OPENAI_API_KEY` nem de
uma senha real de banco - `application.properties` usa valores de fallback óbvios e não
funcionais só para permitir que o contexto do Spring suba em teste (ver
[`docs/security/secret-management.md`](docs/security/secret-management.md) para o porquê disso
ser seguro).

## PostgreSQL via Docker Compose

```bash
docker compose up -d
docker compose ps
docker compose logs database
```

`compose.yaml` sobe um serviço `database` (`postgres:18-alpine`) com volume nomeado, healthcheck
via `pg_isready`, e a porta publicada apenas em `127.0.0.1`. Um script de inicialização
(`docker/postgres/init/01-restrict-app-role.sh`) remove o atributo `SUPERUSER` do usuário da
aplicação logo após o bootstrap - o usuário continua dono do banco `budgeting` (então Flyway/JPA
funcionam normalmente), mas deixa de ser superusuário em qualquer conexão feita depois disso. Ver
[`docs/security/security-controls.md`](docs/security/security-controls.md) para o detalhe.

## Como rodar

```bash
./gradlew bootRun
```

Requer `docker compose up -d` rodando (ou uma instância PostgreSQL equivalente) e um `.env`
carregado no ambiente. Sem `OPENAI_API_KEY` real configurada, os endpoints REST funcionam
normalmente; só o fluxo de voz (`/api/v1/assistant/audio`) depende de uma chave OpenAI válida.

## Como testar

```bash
./gradlew clean test    # unitários + fatiados (MockMvc/Security) + integração (Testcontainers/PostgreSQL)
./gradlew check
```

Exige Docker rodando (para os testes de persistência com Testcontainers) - não exige uma instância
de PostgreSQL rodando manualmente, nem `OPENAI_API_KEY`, nem chamada real à OpenAI. Ver
[`docs/security/security-testing.md`](docs/security/security-testing.md) para o detalhamento por
categoria de teste.

## Testes opcionais e faturáveis do OpenAI

```bash
OPENAI_API_KEY=sk-... ./gradlew openAiIntegrationTest
```

Task Gradle separada, fora de `test`/`check`/`build`, pulada automaticamente se
`OPENAI_API_KEY` não estiver definida, e nunca executada pelo CI. Ver
[`docs/security/security-testing.md`](docs/security/security-testing.md).

## Como rodar os scans de segurança localmente

```bash
# Gitleaks (mesma imagem e digest usados no CI)
docker run --rm -v "$(pwd):/repo" \
  zricethezav/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f \
  detect --source /repo --no-git --redact -v

# Trivy (mesma imagem e digest usados no CI)
docker build -t spring-ai-budgeting-assistant:local .
docker save spring-ai-budgeting-assistant:local -o /tmp/image.tar
docker run --rm -v "/tmp:/workspace" \
  aquasec/trivy@sha256:7cced7cae583819fc7806d4cbc0dbbc7cad18b99f7d3e235192e6da8c091045c \
  image --input /workspace/image.tar --severity HIGH,CRITICAL
```

## Exemplos de endpoints

Todos exigem HTTP Basic (`Authorization: Basic ...`) com `APP_SECURITY_USERNAME`/`APP_SECURITY_PASSWORD`.

**Criar transação (sem IA):**

```http
POST /api/v1/transactions
Content-Type: application/json
Authorization: Basic ...

{
  "description": "Parking payment",
  "amountInCents": 6000,
  "category": "AUTO"
}
```

Resposta `201 Created`, `source` sempre `"REST"`, corpo com `TransactionOutput` (nunca a entidade
JPA).

**Consultar transações por categoria:**

```http
GET /api/v1/transactions?category=AUTO
Authorization: Basic ...
```

Resposta `200 OK`, lista vazia quando não há resultado, restrita ao usuário autenticado.

**Assistente de voz:**

```http
POST /api/v1/assistant/audio
Content-Type: multipart/form-data
Authorization: Basic ...

file=<áudio .mp3/.wav/.m4a, até 5 MB>
```

Resposta `200 OK`, `Content-Type: audio/mpeg`. Upload inválido é rejeitado antes de qualquer
chamada à OpenAI (ver `AudioUploadValidator`).

## Autenticação

HTTP Basic, sessão stateless, sem cookie. **Fora de `localhost`, isso exige HTTPS/TLS** - Basic
só codifica em base64, não criptografa. CSRF está desabilitado deliberadamente (API stateless, sem
sessão de navegador) - motivo completo em
[`docs/security/security-controls.md`](docs/security/security-controls.md).

## Controles de AppSec (resumo)

Cada controle abaixo segue o modelo Ameaça → Controle → Teste → Evidência → Lição, documentado por
extenso em [`docs/security/`](docs/security/):

- Autenticação obrigatória em todo endpoint financeiro, credenciais só via ambiente.
- Validação em duas camadas (Bean Validation + invariantes de domínio) - dinheiro nunca é
  `double`/`float`, sempre `long` em centavos, convertido para exibição com
  `BigDecimal.valueOf(cents, 2)`.
- Upload de áudio validado localmente (tamanho, extensão, media type) antes de qualquer chamada à
  OpenAI.
- Idempotência em duas camadas (aplicação + `UNIQUE` no banco) para chamadas de ferramenta
  duplicadas.
- Tratamento de erro centralizado, sem stack trace/SQL/nome de classe interna na resposta HTTP.
- Container roda como usuário não-root (`spring`, uid 100).
- Papel de aplicação do PostgreSQL sem `SUPERUSER` após o bootstrap.
- Pipeline de CI com Gitleaks, CodeQL, revisão de dependências, scan de imagem com Trivy e geração
  de SBOM - todas as GitHub Actions fixadas por SHA completo (não por tag flutuante); Gitleaks e
  Trivy rodam como imagens Docker oficiais fixadas por digest, não pela Action de terceiros
  correspondente - decisão motivada por um incidente real de cadeia de suprimentos pesquisado
  durante a construção deste projeto (ver
  [`docs/security/threat-model.md`](docs/security/threat-model.md), ameaça 6).

## Modelo de ameaça de prompt injection (resumo)

A transcrição de voz e qualquer argumento de ferramenta proposto pelo modelo são tratados como
entrada não confiável, no mesmo nível de um corpo de requisição HTTP bruto. O prompt de sistema é
orientação, não fronteira de segurança. A fronteira real é: allowlist explícita de ferramentas
(`persistTransaction`, `listTransactionsByCategory` - nada mais), identidade e chave de
idempotência sempre derivadas do contexto confiável do servidor (nunca de um argumento de
ferramenta), e toda regra de negócio validada em Java determinístico antes de qualquer persistência.

Afirmação defensável adotada neste projeto:

> A injeção de prompt pode influenciar o texto gerado pelo modelo, mas os controles implementados
> impedem que o modelo obtenha acesso a segredos, ferramentas indisponíveis, capacidades do sistema
> operacional, detalhes internos do banco de dados, dados de outros usuários, ou operações de
> negócio inválidas.

Detalhamento completo, incluindo as 14 mitigações exigidas mapeadas para código real, em
[`docs/security/prompt-injection.md`](docs/security/prompt-injection.md).

## Limitações de prompt injection

Este projeto não afirma imunidade a prompt injection. O texto final que o assistente fala de volta
ao usuário pode ser influenciado por uma tentativa de injeção (por exemplo, o assistente pode se
recusar a ajudar, ou responder de forma estranha) - isso nunca é tratado como confiável para nada
além de virar áudio para o mesmo usuário autenticado que enviou o pedido. O projeto não foi
submetido a um red team sistemático contra técnicas de jailbreak; a defesa aqui é estrutural
(allowlist de capacidades), não baseada em filtro de conteúdo - por isso ela não depende de prever
toda formulação possível de ataque.

## Fluxo de TDD

Cada fatia significativa foi implementada como RED → GREEN → REFACTOR: um teste falhando primeiro,
implementação mínima depois, teste passando, e só então qualquer ajuste. O relatório final desta
implementação traz ciclos representativos com comando, saída de falha e saída de sucesso reais.
Ordem seguida: conversão monetária → invariantes de domínio → caso de uso de persistência → caso
de uso de listagem → idempotência → mapeamento JPA → comportamento de repositório → validação REST
→ autenticação → validação de upload → adaptador de ferramentas → orquestração de voz →
tratamento de exceção seguro.

## Pipeline DevSecOps

| Workflow | O que faz |
|---|---|
| `.github/workflows/ci.yml` | Build + suíte de testes completa (inclui Testcontainers/PostgreSQL) |
| `.github/workflows/gitleaks.yml` | Varredura de segredos (imagem oficial fixada por digest) |
| `.github/workflows/codeql.yml` | Análise estática para Java |
| `.github/workflows/dependency-review.yml` | Bloqueia PR que introduz dependência vulnerável de severidade alta |
| `.github/workflows/docker-security.yml` | Build da imagem, scan com Trivy, geração de SBOM (CycloneDX) |
| `.github/dependabot.yml` | Atualizações automáticas: Gradle, GitHub Actions, Docker |

Permissões mínimas em cada workflow (`contents: read` por padrão), nenhuma chamada real à OpenAI,
nenhuma senha de produção, nenhum `continue-on-error` sem justificativa.

## Imagem Docker

Build multi-stage (`eclipse-temurin:21-jdk-jammy` para compilar, `eclipse-temurin:21-jre-alpine`
para rodar), usuário não-root dedicado, porta `8080` exposta, sem segredo em build arg ou camada
de imagem. Testes rodam no CI, não durante o build da imagem - o build da imagem não precisa de
Docker-in-Docker para os testes de Testcontainers.

```bash
docker build -t spring-ai-budgeting-assistant .
docker run --rm --entrypoint id spring-ai-budgeting-assistant   # confirma usuário não-root
```

## Limitações conhecidas

- HTTP Basic exige HTTPS fora de `localhost`; este projeto não configura TLS (fora de escopo
  educacional).
- Não há isolamento multiusuário real além do único par de credenciais HTTP Basic configurado -
  o campo `ownerUsername` existe e é respeitado ponta a ponta, mas o projeto não implementa cadastro
  de múltiplos usuários.
- A task `openAiIntegrationTest` existe como esqueleto opt-in, mas não foi executada nem povoada
  com casos reais durante esta implementação (evitar custo de API sem necessidade).
- Nenhuma prova de penetração formal foi conduzida.
- O scan de Trivy contra a imagem atual (após o hardening documentado no relatório de hardening)
  não reporta nenhum achado HIGH/CRITICAL — nem no SO base, nem nas dependências Java. Isso é o
  estado em um ponto no tempo, não uma garantia permanente: novas CVEs podem ser publicadas a
  qualquer momento contra as mesmas versões; é para isso que o Trivy continua rodando no CI a cada
  push/PR (`docker-security.yml`), não só nesta validação manual.

## Lições aprendidas

- Testar explicitamente o caminho "sem credenciais" em todo endpoint evita que um serviço fique
  aberto por omissão.
- Injeção de prompt é um problema de dados, não de texto do prompt de sistema - a defesa que
  funciona é "o modelo não tem ferramenta capaz de fazer a coisa perigosa", não uma frase mais
  cuidadosa no prompt.
- Idempotência precisa de duas camadas: uma checagem de aplicação para o caminho normal, e uma
  restrição de banco para quando esse caminho normal for contornado ou estiver errado.
- Uma tag de versão numa GitHub Action de terceiros não é fronteira de segurança - pode ser movida
  por quem controla a conta que a publica. Fixar por SHA/digest é o que realmente protege.
- Gerar o esqueleto do projeto via Spring Initializr e inspecionar os `.jar` baixados custou menos
  tempo - e produziu menos erro - do que escrever `build.gradle` de memória para uma combinação de
  versões (Spring Boot 4 / Spring AI 2 / Testcontainers 2) lançada há pouco tempo.

## Roadmap

- Implementar casos reais na task `openAiIntegrationTest`.
- Avaliar suporte a múltiplos usuários reais (hoje o isolamento por `ownerUsername` existe e é
  testado, mas só há um par de credenciais configurável).
- Considerar TLS/HTTPS para além de uso local.
- Acompanhar novas releases do Spring Boot 4.1.x / Spring AI 2.x que passem a gerenciar Netty
  numa versão igual ou superior à fixada manualmente hoje (`io.netty:netty-bom:4.2.16.Final`),
  para então remover o override e voltar a depender só do BOM.
