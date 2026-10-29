  <div align="center">

<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&weight=600&size=17&duration=2500&pause=700&color=2F81F7&center=true&vCenter=true&width=1000&lines=%5BSYSTEM%5D+BANKING+API+ONLINE;%5BSYSTEM%5D+LOADING+SECURITY+MODULE...;%5BSYSTEM%5D+SPRING+SECURITY+CONNECTED;%5BSYSTEM%5D+WEB+INTERFACE+READY;%5BSYSTEM%5D+MYSQL+%26+PROMETHEUS+CONNECTED;%5BSYSTEM%5D+READY+FOR+TRANSACTIONS..." alt="Typing Animation">

<h1>SISTEMA DE BANCO</h1>

<p>
  <strong>KAIQUE JACOB</strong> · Java 25 · Spring Boot 4 · Spring Security · MySQL · Docker
</p>

</div>

---

<div align="center">



![Java](https://img.shields.io/badge/Java%2025-0B0F14?style=for-the-badge&logo=openjdk&logoColor=2F81F7)




![Spring Boot](https://img.shields.io/badge/Spring%20Boot%204-0B0F14?style=for-the-badge&logo=springboot&logoColor=2F81F7)




![Spring Security](https://img.shields.io/badge/Spring%20Security-0B0F14?style=for-the-badge&logo=springsecurity&logoColor=2F81F7)




![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-0B0F14?style=for-the-badge&logo=spring&logoColor=2F81F7)




![MySQL](https://img.shields.io/badge/MySQL-0B0F14?style=for-the-badge&logo=mysql&logoColor=2F81F7)




![HTML](https://img.shields.io/badge/HTML5-0B0F14?style=for-the-badge&logo=html5&logoColor=2F81F7)




![CSS](https://img.shields.io/badge/CSS3-0B0F14?style=for-the-badge&logo=css&logoColor=2F81F7)




![JavaScript](https://img.shields.io/badge/JavaScript-0B0F14?style=for-the-badge&logo=javascript&logoColor=2F81F7)




![Docker](https://img.shields.io/badge/Docker-0B0F14?style=for-the-badge&logo=docker&logoColor=2F81F7)




![Prometheus](https://img.shields.io/badge/Prometheus-0B0F14?style=for-the-badge&logo=prometheus&logoColor=2F81F7)




![Grafana](https://img.shields.io/badge/Grafana-0B0F14?style=for-the-badge&logo=grafana&logoColor=2F81F7)



</div>

## `SOBRE`

Sistema bancário desenvolvido em **Java 25** com **Spring Boot 4**, **Spring Security**, **Spring Data JPA** e **MySQL**, composto por uma API REST e uma interface web em HTML, CSS e JavaScript.

O sistema permite:

- 🏦 **Cadastro de contas** corrente, poupança e investimento, com endereço preenchido automaticamente via **ViaCEP**.
- 💰 **Depósitos e saques** com limite diário por tipo de conta, franquia de saques, taxas e histórico de transações.
- 🔁 **Transferências** entre contas, com débito e crédito na mesma transação de banco.
- 🔐 **Autenticação HTTP Basic** com usuários `CLIENTE` e `ADMIN`.
- 🧾 **Extrato** e consulta da própria conta por meio de rotas `/me`.
- 🛡️ **Controle de acesso** por papel: o CPF do cliente vem sempre do login, nunca da URL ou do corpo da requisição.
- 🖥️ **Interface web** servida pelo próprio Spring Boot, com área do cliente e painel administrativo.
- ⚙️ **Controle de concorrência** com lock otimista (`@Version`).
- 📈 **Observabilidade** com Actuator, Prometheus e Grafana.
- 📘 **Documentação interativa** com OpenAPI/Swagger (perfil `dev`).

Os dados são persistidos em **MySQL**, estruturados sob princípios de **Domain-Driven Design (DDD)** e arquitetura em camadas.

---

## `INTERFACE WEB`

<!-- Adicione um print em docs/images/interface.png e descomente:
<div align="center">
<img src="docs/images/interface.png" alt="Interface do Sistema de Banco" width="1100">
</div>
-->

A interface é servida pelo próprio Spring Boot em `src/main/resources/static`, sem framework de front-end (HTML, CSS e JavaScript com módulos ES).

Acesse `http://localhost:8080/` com a aplicação em execução.

| Área | Recursos |
|---|---|
| **Acesso** | Login com CPF ou `admin` e cadastro de conta com consulta de endereço pelo CEP |
| **Visão geral** | Saldo, tipo de conta, CPF, endereço e atalhos para as operações |
| **Extrato** | Histórico de lançamentos da conta |
| **Operações** | Transferência, depósito e saque |
| **Administração** | Listagem de contas por saldo, cobrança de taxa de manutenção e encerramento de conta |

Detalhes de funcionamento:

```text
[✓] Layout responsivo para desktop e celular
[✓] Menu de administração exibido apenas para o usuário admin
[✓] Notificações de sucesso e erro em toasts
[✓] Mensagens de erro vindas do ProblemDetail da API
[✓] Credenciais mantidas apenas em memória; recarregar a página exige novo login
```

---

## `FLUXO DA API`

```text
                 ┌──────────────────────────────────────────┐
                 │          NAVEGADOR / CLIENTE HTTP        │
                 │  Interface web · HTTP Basic · JSON       │
                 └────────────────────┬─────────────────────┘
                                      │
                                      ▼
                 ┌──────────────────────────────────────────┐
                 │              PRESENTATION                │
                 │ Controllers · DTOs · Validation · Errors │
                 └────────────────────┬─────────────────────┘
                                      │
                                      ▼
                 ┌──────────────────────────────────────────┐
                 │              APPLICATION                 │
                 │      ContaService · RegistroDeConta      │
                 └────────────────────┬─────────────────────┘
                                      │
                                      ▼
                 ┌──────────────────────────────────────────┐
                 │                 DOMAIN                   │
                 │  ContaBancaria · Transacao · Regras      │
                 └────────────────────┬─────────────────────┘
                                      │
                                      ▼
                 ┌──────────────────────────────────────────┐
                 │             INFRASTRUCTURE               │
                 │   JPA/MySQL · ViaCEP · Security · Docs   │
                 └──────────────────────────────────────────┘
```

---

## `CONCEITOS APLICADOS`

```text
SISTEMA DE BANCO (SPRING BOOT 4 + JAVA 25)

[✓] Arquitetura em camadas com Domain-Driven Design (DDD)
[✓] Separação entre application, domain, infrastructure e presentation
[✓] Domínio rico com ContaBancaria, ContaCorrente, Poupança e Investimento
[✓] Padrão Factory para criação de contas
[✓] Ports & Adapters com ContaDAO e CredencialDAO
[✓] Persistência com Spring Data JPA, Hibernate e MySQL
[✓] Controle de concorrência com lock otimista (@Version)
[✓] Transações com Spring @Transactional
[✓] Autenticação e autorização com Spring Security
[✓] HTTP Basic stateless com papéis CLIENTE e ADMIN
[✓] Senhas armazenadas com BCrypt
[✓] Validação de entrada com Bean Validation e validador de CPF próprio
[✓] Tratamento de erros com ProblemDetail (RFC 9457)
[✓] Integração externa com ViaCEP (timeouts configurados e cache em memória)
[✓] Interface web responsiva em HTML, CSS e JavaScript (módulos ES)
[✓] Documentação OpenAPI e Swagger UI
[✓] Observabilidade com Actuator, Micrometer, Prometheus e Grafana
[✓] Testes unitários, de integração e de concorrência com JUnit 5
```

---

## `ENDPOINTS DA API`

| Operação | Método / Endpoint | Acesso | Descrição |
|---|---|---|---|
| 🖥️ **Interface web** | `GET /` | Público | Interface do Internet Banking |
| 🏦 **Criar conta** | `POST /contas` | Público | Cadastra conta, endereço (via CEP) e credencial de acesso |
| 👤 **Minha conta** | `GET /contas/me` | `CLIENTE` | Retorna os dados da conta autenticada |
| 📝 **Meu extrato** | `GET /contas/me/extrato` | `CLIENTE` | Retorna o histórico de transações e os detalhes do tipo de conta |
| 💰 **Depositar** | `POST /contas/me/depositos` | `CLIENTE` | Realiza depósito na conta autenticada |
| 🏧 **Sacar** | `POST /contas/me/saques` | `CLIENTE` | Realiza saque na conta autenticada |
| 🔁 **Transferir** | `POST /transferencias` | `CLIENTE` | Transfere da conta autenticada para outra conta |
| 📊 **Listar contas** | `GET /contas?pagina=0&tamanho=20` | `ADMIN` | Lista contas ordenadas por saldo (tamanho máximo: 100) |
| 🔍 **Buscar conta** | `GET /contas/admin/{cpf}` | `ADMIN` | Consulta uma conta pelo CPF |
| 🧾 **Extrato por CPF** | `GET /contas/admin/{cpf}/extrato` | `ADMIN` | Consulta o extrato de qualquer conta |
| ❌ **Remover conta** | `DELETE /contas/admin/{cpf}` | `ADMIN` | Remove uma conta sem histórico e com saldo zerado |
| 💸 **Cobrar taxa** | `POST /contas/admin/{cpf}/taxa-manutencao` | `ADMIN` | Cobra a taxa de manutenção do tipo de conta |
| ❤️ **Health check** | `GET /actuator/health` | Público | Verifica o estado da aplicação |
| 📈 **Métricas** | `GET /actuator/prometheus` | `ADMIN` | Expõe métricas para o Prometheus |
| 📘 **Swagger UI** | `GET /swagger-ui.html` | Apenas perfil `dev` | Documentação interativa |

---

## `REGRAS DE NEGÓCIO`

| Tipo de conta | Taxa de manutenção | Limite diário de saque | Particularidades |
|---|---|---|---|
| **Corrente** | R$ 12,00 | R$ 2.000,00 | 2 saques gratuitos por mês; a partir do 3º, taxa de R$ 10,00 por saque |
| **Poupança** | Isenta | R$ 1.000,00 | Rendimento mensal estimado de 0,5% do saldo |
| **Investimento** | R$ 25,00 | R$ 5.000,00 | Rendimento mensal estimado de 1,2% do saldo e taxa de administração de R$ 20,00 exibida no extrato |

Regras gerais:

```text
[✓] A taxa de saque é registrada como transação separada e não consome o limite diário
[✓] Se o saldo não cobre saque + taxa, nada é debitado
[✓] Transferências não consomem a franquia de saques nem o limite diário
[✓] Transferência para a própria conta é rejeitada
[✓] Conta só pode ser removida se tiver saldo zerado e nenhuma transação
[✓] Abertura de conta com saldo inicial gera a transação ABERTURA_CONTA
```

---

## `TRATAMENTO DE ERROS`

Os erros seguem o formato `ProblemDetail` (RFC 9457). Mensagens internas (banco, ViaCEP, exceções inesperadas) nunca são devolvidas ao cliente; ficam apenas no log.

| Status | Quando acontece |
|---|---|
| `400` | Validação de campos, CPF ou e-mail inválido, valor inválido |
| `401` / `403` | Sem credencial, senha incorreta ou papel sem permissão |
| `404` | Conta não encontrada |
| `409` | CPF já cadastrado, conta com saldo ou histórico, conflito de concorrência ou lock |
| `422` | Saldo insuficiente ou limite diário de saque excedido |
| `502` | Falha ao consultar o ViaCEP |
| `503` | Banco de dados indisponível |
| `500` | Erro inesperado (mensagem genérica) |

---

## `SEGURANÇA`

A API utiliza **HTTP Basic** com sessão stateless.

```text
[✓] Cadastro público apenas para criação de conta
[✓] CLIENTE acessa somente a própria conta por /contas/me
[✓] ADMIN acessa listagem, consulta, exclusão e taxa de manutenção
[✓] Rotas não mapeadas são negadas por padrão (denyAll)
[✓] Apenas os arquivos estáticos da interface são públicos
[✓] Senhas armazenadas com BCrypt
[✓] Mensagem idêntica para usuário inexistente e senha incorreta
[✓] Administrador criado no primeiro start, a partir de variável de ambiente
[✓] CSRF desativado por ser uma API stateless sem cookies
[✓] Swagger liberado apenas no perfil dev
```

> **Limitação:** HTTP Basic envia credenciais em todas as requisições. Em produção, a API deve ser exposta exclusivamente por HTTPS.

---

## `COMO EXECUTAR`

### 1. Pré-requisitos

- **Java 25**
- **Maven 3.9+**
- **Docker** e **Docker Compose**

### 2. Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `ADMIN_PASSWORD` | Senha do usuário `admin` (mínimo de 8 caracteres). Sem ela, o administrador não é criado. | — |
| `DB_URL` | URL de conexão com o MySQL | `jdbc:mysql://localhost:3307/banco_db` |
| `DB_USER` | Usuário do banco | `root` |
| `DB_PASSWORD` | Senha do banco (também usada como senha root no Docker Compose) | `root` |
| `GRAFANA_USER` / `GRAFANA_PASSWORD` | Credenciais do Grafana | `admin` / `admin` |

> Os padrões `root/root` e `admin/admin` existem apenas para desenvolvimento local. Em qualquer outro ambiente, defina valores próprios.

### 3. Configurar a senha do Prometheus

O endpoint `/actuator/prometheus` exige o usuário `admin`, então o Prometheus precisa da senha dele. Ela é lida de um arquivo ignorado pelo Git, e nunca fica escrita no `prometheus.yml`:

```bash
# Linux/macOS
mkdir -p secrets
printf '%s' "$ADMIN_PASSWORD" > secrets/prometheus_admin_password

# Windows PowerShell
New-Item -ItemType Directory -Force secrets | Out-Null
Set-Content -NoNewline -Path secrets/prometheus_admin_password -Value $env:ADMIN_PASSWORD
```

### 4. Subir a infraestrutura

```bash
docker compose up -d db prometheus grafana
```

O MySQL será iniciado na porta `3307`, o Prometheus em `9090` e o Grafana em `3000`.

O `schema.sql` cria as tabelas na **primeira** inicialização do volume do MySQL (a aplicação usa `ddl-auto: validate`, então o schema precisa existir). Para recriar o banco do zero:

```bash
docker compose down -v
docker compose up -d db prometheus grafana
```

### 5. Configurar a senha do administrador

```bash
# Linux/macOS
export ADMIN_PASSWORD=SuaSenhaForte#1

# Windows PowerShell
$env:ADMIN_PASSWORD="SuaSenhaForte#1"
```

O administrador (`admin`) é criado automaticamente no primeiro start, caso ainda não exista.

### 6. Executar a aplicação

```bash
mvn spring-boot:run
```

A API e a interface web estarão disponíveis em: `http://localhost:8080`.

### 7. Executar com Swagger

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Acesse:

```text
http://localhost:8080/swagger-ui.html
```

---

<details>
<summary><strong>💡 Exemplos de requisição (cURL)</strong></summary>

<br>

#### Criar conta (público)
```bash
curl -X POST http://localhost:8080/contas \
  -H "Content-Type: application/json" \
  -d '{
    "titular": "Maria Souza",
    "cpf": "52998224725",
    "email": "maria@example.com",
    "cep": "01001000",
    "tipo": "CORRENTE",
    "saldoInicial": 100.00,
    "senha": "SenhaForte#1"
  }'
```

#### Consultar a própria conta (o login é o CPF)
```bash
curl -u 52998224725:SenhaForte#1 http://localhost:8080/contas/me
```

#### Depositar
```bash
curl -X POST http://localhost:8080/contas/me/depositos \
  -u 52998224725:SenhaForte#1 \
  -H "Content-Type: application/json" \
  -d '{"valor": 250.00}'
```

#### Transferir
```bash
curl -X POST http://localhost:8080/transferencias \
  -u 52998224725:SenhaForte#1 \
  -H "Content-Type: application/json" \
  -d '{"cpfDestino": "11144477735", "valor": 100.00}'
```

#### Listar contas (admin)
```bash
curl -u admin:SuaSenhaForte#1 "http://localhost:8080/contas?pagina=0&tamanho=20"
```

</details>

---

## `TESTES AUTOMATIZADOS`

O projeto inclui testes unitários, de repositório, de segurança, de concorrência e de integração ponta a ponta. Os testes usam **H2 em memória** (perfil `test`), então não precisam do MySQL.

```bash
mvn test
```

Cenários cobertos:

```text
[✓] Regras de depósito, saque, transferência, franquia de saques e taxas
[✓] Limite diário de saque restaurado ao recarregar a conta do banco
[✓] Criação de contas por tipo
[✓] Persistência e consulta com JPA
[✓] Rollback quando a segunda transação de uma transferência falha
[✓] Conflito de concorrência com lock otimista (@Version)
[✓] Transferências simultâneas sem perda de atualização
[✓] Criação simultânea do mesmo CPF: uma vence e a outra recebe 409
[✓] Autenticação HTTP Basic
[✓] Autorização por CLIENTE e ADMIN
[✓] Proteção contra acesso à conta de outro usuário
[✓] Cadastro público e criação de credencial
[✓] Tratamento global de erros sem vazamento de mensagens internas
[✓] Integração com ViaCEP (com cache e sem cachear falhas)
```

> A interface web ainda não possui testes automatizados.

---

## `ESTRUTURA DO PROJETO`

A estrutura segue os princípios de **Domain-Driven Design**, **Clean Architecture** e **Ports & Adapters**:

```text
src/main/java/br/com/sistemabanco/
├── application/              # Casos de uso e orquestração do negócio
│   ├── ContaService.java
│   └── RegistroDeConta.java
│
├── domain/                   # Núcleo do negócio, sem dependência de framework web
│   ├── exception/            # Exceções de negócio
│   ├── factory/              # Criação de contas
│   ├── model/                # ContaBancaria, Transacao, Endereco e tipos
│   ├── repository/           # Portas: ContaDAO e CredencialDAO
│   ├── service/              # Contrato do serviço de CEP
│   └── util/                 # Validador (CPF, e-mail e nome)
│
├── infrastructure/           # Detalhes técnicos e adaptadores externos
│   ├── client/               # Integração com ViaCEP
│   ├── config/               # Configurações de cliente e propriedades
│   ├── openapi/              # Configuração do Swagger/OpenAPI
│   ├── persistence/          # JPA: entities, mappers e repositórios
│   └── security/             # Usuários, papéis, BCrypt e Spring Security
│
└── presentation/             # Camada de entrada HTTP
    ├── controller/           # Controllers REST
    ├── handler/              # Tratamento global de erros
    ├── mapper/               # Conversão domínio -> DTO
    ├── requests/             # DTOs de entrada
    ├── responses/            # DTOs de saída
    └── validation/           # Anotação @Cpf e seu validador

src/main/resources/
├── static/                   # Interface web
│   ├── index.html
│   ├── css/style.css
│   └── js/                   # app.js, api.js e ui.js
├── application.yml
└── application-dev.yml
```

<details>
<summary><strong>Visualizar árvore completa de arquivos</strong></summary>

```text
sistema-de-banco/
├── src/
│   ├── main/
│   │   ├── java/br/com/sistemabanco/
│   │   │   ├── application/
│   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   ├── factory/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   └── util/
│   │   │   ├── infrastructure/
│   │   │   │   ├── client/
│   │   │   │   ├── config/
│   │   │   │   ├── openapi/
│   │   │   │   ├── persistence/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── mapper/
│   │   │   │   │   └── repository/
│   │   │   │   └── security/
│   │   │   ├── presentation/
│   │   │   │   ├── controller/
│   │   │   │   ├── handler/
│   │   │   │   ├── mapper/
│   │   │   │   ├── requests/
│   │   │   │   ├── responses/
│   │   │   │   └── validation/
│   │   │   └── SistemaBancoApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── index.html
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       ├── java/br/com/sistemabanco/
│       │   ├── application/
│       │   ├── domain/
│       │   ├── infrastructure/
│       │   ├── integration/
│       │   └── presentation/
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml
├── prometheus.yml
├── pom.xml
├── schema.sql
└── README.md
```

</details>

---

## `OBSERVABILIDADE`

A aplicação expõe métricas no formato Prometheus por meio do Actuator:

```text
http://localhost:8080/actuator/prometheus
```

O Prometheus (rodando em container) coleta as métricas de `host.docker.internal:8080`, ou seja, da aplicação rodando na sua máquina, e o Grafana permite visualizá-las em dashboards. No Grafana, adicione o Prometheus como fonte de dados usando a URL `http://prometheus:9090`.

| Serviço | URL |
|---|---|
| API e interface web | `http://localhost:8080` |
| Swagger UI (perfil `dev`) | `http://localhost:8080/swagger-ui.html` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

---

## `TECNOLOGIAS`

<div align="center">

<pre align="center">
[SYSTEM STATUS]

Language  : JAVA 25
Framework : SPRING BOOT 4
Frontend  : HTML, CSS & JAVASCRIPT
Security  : SPRING SECURITY + HTTP BASIC + BCRYPT
Database  : MYSQL + SPRING DATA JPA
Docs      : SPRINGDOC OPENAPI + SWAGGER UI
Metrics   : ACTUATOR + MICROMETER + PROMETHEUS
Dashboard : GRAFANA
Runtime   : DOCKER + DOCKER COMPOSE
Testing   : JUNIT 5 + MOCKITO + H2
Build     : MAVEN

STATUS    : READY_TO_BUILD

&gt; keep_learning();
&gt; keep_building();
</pre>

</div>

---

<div align="center">

### `SYSTEM // END`

**Java • Spring Boot • Spring Security • MySQL • HTML • CSS • JavaScript • Docker • Prometheus • Grafana**

</div>