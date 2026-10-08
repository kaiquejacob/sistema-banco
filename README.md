<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&weight=600&size=18&duration=2500&pause=700&color=2F81F7&center=true&vCenter=true&width=780&lines=%5BSYSTEM%5D+BANKING+SYSTEM+ONLINE;%5BSYSTEM%5D+LOADING+JDBC+%26+MYSQL...;%5BSYSTEM%5D+MOCKITO+SUITE+ACTIVE;%5BSYSTEM%5D+READY+FOR+TRANSACTIONS..." alt="Typing Animation">

<img src="https://capsule-render.vercel.app/api?type=waving&height=160&color=0:05070A,50:0B1F3A,100:061A36&text=SISTEMA%20BANC%C3%81RIO&fontSize=40&fontColor=E6F1FF&fontAlignY=40&desc=KAIQUE%20JACOB%20%7C%20JAVA%20%2B%20JDBC%20%2B%20MYSQL%20%2B%20MOCKITO&descAlignY=65&descSize=15" width="100%" alt="Sistema Bancário Banner">

</div>

---

<div align="center">

![Java](https://img.shields.io/badge/Java%2025-0B0F14?style=for-the-badge&logo=openjdk&logoColor=2F81F7)
![MySQL](https://img.shields.io/badge/MySQL-0B0F14?style=for-the-badge&logo=mysql&logoColor=2F81F7)
![Docker](https://img.shields.io/badge/Docker-0B0F14?style=for-the-badge&logo=docker&logoColor=2F81F7)
![Maven](https://img.shields.io/badge/Maven-0B0F14?style=for-the-badge&logo=apachemaven&logoColor=2F81F7)
![Status](https://img.shields.io/badge/Status-Evolving-0B0F14?style=for-the-badge&logo=github&logoColor=2F81F7)

</div>

## `SOBRE`

Sistema de gerenciamento de contas bancárias via console desenvolvido em **Java**, focado em arquitetura limpa e padrões de mercado. A aplicação permite a criação de contas (Corrente, Poupança e Investimento) com integração em tempo real à API REST do ViaCEP para preenchimento automático de endereço, controle transacional de transferências, tratamento robusto de exceções e persistência relacional em banco de dados **MySQL**.

Projeto utilizado como aplicação prática evolutiva dos conhecimentos da **Maratona Java (DevDojo)** e bootcamps de desenvolvimento backend, migrando de persistência em arquivos para JDBC com padrão **DAO**, uso de `Optional`, **Docker Compose** para infraestrutura local e testes unitários automatizados com **JUnit 5 e Mockito**.

---

## `CONCEITOS APLICADOS`

```text
SISTEMA BANCÁRIO

[✓] Arquitetura em Camadas (Config, Domain, Repository, Service, Exception, Util)
[✓] Polimorfismo em Enums (TipoConta com métodos abstratos para regras por modalidade)
[✓] Pattern Data Access Object (DAO) para isolamento total da camada de persistência
[✓] Gestão estrita de recursos JDBC (try-with-resources prevenindo vazamento de conexões)
[✓] Programação Funcional com Optional<T> para eliminação de retornos nulos
[✓] Controle Transacional JDBC (commit, rollback e gerenciamento de auto-commit manual)
[✓] Padrão Factory Method (ContaFactory para instanciação padronizada de entidades)
[✓] Consumo de API REST Externa (Integração com ViaCEP via java.net.http.HttpClient)
[✓] Exceções Customizadas Unchecked (Herança de RuntimeException isolando regras de negócio)
[✓] Testes Unitários Automatizados (Suíte com JUnit 5 + Mockito e MockitoExtension)
[✓] Gestão de Build e Dependências (Apache Maven via pom.xml)
[✓] Validações de Domínio com RegEx (Validação de CPF com dígito verificador e e-mail)
[✓] Containerização de Infraestrutura (Docker Compose subindo instância MySQL 8.0)
[✓] Stream API & Comparator (Ordenação dinâmica de listagens de contas)

```

---

## `FUNCIONALIDADES`

| Operação | Descrição |
| --- | --- |
| 📝 Criar conta | Cadastra conta consumindo a API REST do ViaCEP para obter o endereço via HTTP |
| 💰 Depositar | Credita saldo na conta informada e registra a transação no banco de dados |
| 💸 Sacar | Debita saldo respeitando as taxas e limites diários específicos do `TipoConta` |
| 📊 Ver extrato | Exibe dados do titular, endereço completo e histórico das transações |
| 📋 Listar contas | Exibe todas as contas ativas ordenadas por saldo de forma decrescente |
| ❌ Remover conta | Exclui o cadastro do banco — permitido exclusivamente para contas com saldo zerado |
| 🔄 Transferir | Operação atômica entre duas contas com garantia de transação ACID no MySQL |

---

## `EXCEÇÕES CUSTOMIZADAS`

| Exceção | Motivo |
| --- | --- |
| `br.com.sistemabanco.domain.exception.SaldoInsuficienteException` | Saldo insuficiente para realizar a operação de saque ou transferência |
| `br.com.sistemabanco.domain.exception.ContaNaoEncontradaException` | CPF pesquisado não corresponde a nenhuma conta registrada no banco |
| `br.com.sistemabanco.domain.exception.CpfInvalidoException` | CPF informado falha no algoritmo de validação de dígitos verificadores |
| `br.com.sistemabanco.domain.exception.EmailInvalidoException` | Endereço de e-mail não atende ao padrão da expressão regular |
| `br.com.sistemabanco.domain.exception.ContaComSaldoException` | Tentativa de remoção de conta mantendo saldo superior a zero |
| `br.com.sistemabanco.domain.exception.CpfJaCadastradoException` | Tentativa de cadastrar um CPF já existente |
| `br.com.sistemabanco.domain.exception.ServicoCepException` | Falha na comunicação HTTP ou de rede durante a consulta à API do ViaCEP |

*Todas as exceções estendem `RuntimeException` (unchecked), garantindo o isolamento da camada de apresentação sem poluir as assinaturas dos métodos com `throws`.*

---

## `PERSISTÊNCIA & INTEGRAÇÃO`

* **Banco de Dados Relacional**: Persistência realizada em **MySQL 8.0** gerenciado via **Docker Compose**.
* **Camada JDBC**: Acesso aos dados implementado manualmente através de `PreparedStatement` e `ResultSet`.
* **Prevenção de Leaks**: Todos os recursos de banco (`Connection`, `PreparedStatement`, `ResultSet`) utilizam o bloco **try-with-resources**, garantindo o encerramento das conexões.
* **Consultas Seguras**: Utilização do tipo `Optional<ContaBancaria>` na interface `ContaDAO`, forçando o tratamento funcional na camada de serviço via `.orElseThrow()`.
* **Integração com API REST**: A classe `CepServiceImpl` consome a API do **ViaCEP** através do `HttpClient` nativo do Java, realizando a requisição GET e parse do payload JSON.

---

## `ESTRUTURA`

```text
sistema-de-banco/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── br/com/sistemabanco/
│           ├── Main.java
│           ├── application/
│           │   ├── ContaService.java
│           │   └── ExtratoService.java
│           ├── domain/
│           │   ├── exception/
│           │   │   ├── ContaComSaldoException.java
│           │   │   ├── ContaNaoEncontradaException.java
│           │   │   ├── CpfInvalidoException.java
│           │   │   ├── CpfJaCadastradoException.java
│           │   │   ├── EmailInvalidoException.java
│           │   │   ├── SaldoInsuficienteException.java
│           │   │   └── ServicoCepException.java
│           │   ├── factory/
│           │   │   └── ContaFactory.java
│           │   ├── model/
│           │   │   ├── ContaBancaria.java
│           │   │   ├── ContaCorrente.java
│           │   │   ├── ContaInvestimento.java
│           │   │   ├── ContaPoupanca.java
│           │   │   ├── Endereco.java
│           │   │   ├── TipoConta.java
│           │   │   ├── TipoTransacao.java
│           │   │   └── Transacao.java
│           │   ├── repository/
│           │   │   └── ContaDAO.java
│           │   └── service/
│           │       └── CepService.java
│           ├── infrastructure/
│           │   ├── client/
│           │   │   └── CepServiceImpl.java
│           │   ├── config/
│           │   │   └── ConexaoBanco.java
│           │   └── persistence/
│           │       └── ContaDAOImpl.java
│           └── util/
│               └── Validador.java
│   └── test/
│       └── java/
│           └── br/com/sistemabanco/
│               ├── ContaBancariaTest.java
│               ├── ContaServiceTest.java
│               └── ValidadorTest.java
├── docker-compose.yml
├── .gitignore
├── pom.xml
└── README.md

```

---


## `TECNOLOGIA`


<pre align="center">
[SYSTEM STATUS]

Language   : JAVA 25
Database   : MYSQL 8.0 (DOCKER)
Testing    : JUNIT 5 + MOCKITO
Integration: VIACEP REST API (HTTPCLIENT)
Build      : APACHE MAVEN
Status     : EVOLVING

</pre>

<br>
