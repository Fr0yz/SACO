# SACO — Sistema de Apoio Clínico Odontológico

Aplicação desktop JavaFX para gestão básica de pacientes e fluxo clínico (pacientes, dentistas e anamnese). O projeto utiliza Java 17, JavaFX, Maven e integração com MySQL. Persistência é feita via DAO simples com JDBC; bibliotecas de JPA/Hibernate estão listadas no `pom.xml`, mas a implementação atual usa JDBC direto (ver `dao.Conexao`).

> Observação: o código e os nomes dos arquivos estão em Português. Este README segue a mesma convenção.

---

## Visão geral
- Interface construída com JavaFX e FXML (`src/main/resources/*.fxml`).
- Tela principal carrega `Main.fxml` a partir da classe de entrada `Main`.
- Camadas básicas:
  - `controller/*`: lida com eventos de UI (ex.: `PacienteController`, `AnamneseController`, `MainController`).
  - `dao/*`: acesso a dados via JDBC (ex.: `CadastroPessoaDao`, `Conexao`).
  - `model/*`: modelos de domínio (ex.: `Pessoa`, `Paciente`, `Dentista`).
  - `service/*`: espaço para regras de negócio (há um `CadastroPessoaService`, mas o uso é mínimo no momento).
- Banco: MySQL. Parâmetros de conexão hoje estão hardcoded em `dao.Conexao` (ver seção Variáveis de Ambiente/TODO).

---

## Stack técnica
- Linguagem: Java 17
- UI: JavaFX 21 (controls, fxml)
- Build/gerência de dependências: Maven
- DB: MySQL 8 (driver `mysql-connector-j`)
- Logging: SLF4J Simple (runtime)
- Lombok (provided) — anotações disponíveis, mas o código atual usa principalmente POJOs
- Dependências listadas em `pom.xml`:
  - `org.openjfx:javafx-controls`, `org.openjfx:javafx-fxml`
  - `com.mysql:mysql-connector-j`
  - `jakarta.persistence:jakarta.persistence-api` e `org.hibernate.orm:hibernate-core` (presentes mas não utilizados no código atual)
  - `org.projectlombok:lombok`
  - `org.slf4j:slf4j-simple`

---

## Requisitos
- JDK 17 instalado e no PATH
- Maven 3.8+ (ou superior)
- MySQL 8 em execução
- Acesso para criar/usar um schema (por padrão o código aponta para `pi_athur`)

---

## Configuração
1. Clone o repositório.
2. Configure o banco de dados MySQL:
   - Crie o schema (se ainda não existir):
     ```sql
     CREATE DATABASE pi_athur CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
     ```
   - Crie as tabelas esperadas pelo DAO (não há migrations neste repositório). 
     - TODO: Documentar o DDL das tabelas usadas por `CadastroPessoaDao` (ex.: `TB_PESSOA`, `TB_DENTISTA`, `TB_PACIENTE`).
3. Configure as credenciais de conexão:
   - Atualmente definidas em `src/main/java/dao/Conexao.java`:
     ```java
     private static final String URL = "jdbc:mysql://localhost:3306/pi_athur";
     private static final String USER = "root";
     private static final String PASSWORD = "iarc1001";
     ```
   - Recomenda-se mover para variáveis de ambiente ou arquivo de configuração externo (ver seção Variáveis de Ambiente/TODO).

---

## Como executar
Há duas formas recomendadas: via Maven (com JavaFX plugin) ou via IDE.

### Via Maven (recomendado)
- Executar a aplicação JavaFX:
  ```bash
  mvn clean javafx:run
  ```
  O `pom.xml` usa `org.openjfx:javafx-maven-plugin` e aponta `Main` como `mainClass`.

- Empacotar:
  ```bash
  mvn clean package
  ```
  Isso gera `target/SACO-1.0-SNAPSHOT.jar`. Observação: rodar o JAR diretamente pode requerer parâmetros de módulos JavaFX. O plugin `javafx:run` cuida disso automaticamente.

### Via IDE
- Importar como projeto Maven.
- Garantir JDK 17 como SDK do projeto.
- Marcar `src/main/resources` como Resources Root (geralmente Maven já faz).
- Rodar a classe `Main`.

---

## Scripts e comandos úteis
- `mvn javafx:run` — executa a aplicação com JavaFX configurado
- `mvn clean package` — compila e empacota
- `mvn clean` — limpa artefatos de build

> Não há scripts adicionais no repositório além dos alvos Maven.

---

## Pontos de entrada
- Classe principal: `src/main/java/Main.java`
  - Carrega `Main.fxml` de `src/main/resources/Main.fxml`

---

## Variáveis de ambiente e configuração
Atualmente as credenciais de banco estão no código (`dao.Conexao`). Recomenda-se externalizar:
- Variáveis sugeridas:
  - `DB_URL` (ex.: `jdbc:mysql://localhost:3306/pi_athur`)
  - `DB_USER`
  - `DB_PASSWORD`
- TODO: Ajustar `dao.Conexao` para ler dessas variáveis (ou de `application.properties`) e documentar fallback.

---

## Testes
- Não há testes automatizados no repositório neste momento.
- TODO: Adicionar testes (ex.: JUnit 5) para `dao` e controllers desacoplados, e configurar `maven-surefire-plugin`.

---

## Estrutura do projeto
```
SACO/
├─ pom.xml
├─ src/
│  └─ main/
│     ├─ java/
│     │  ├─ Main.java
│     │  ├─ controller/
│     │  │  ├─ AnamneseController.java
│     │  │  ├─ MainController.java
│     │  │  └─ PacienteController.java
│     │  ├─ dao/
│     │  │  ├─ CadastroPessoaDao.java
│     │  │  └─ Conexao.java
│     │  ├─ model/
│     │  │  ├─ Dentista.java
│     │  │  ├─ Paciente.java
│     │  │  └─ Pessoa.java
│     │  └─ service/
│     │     └─ CadastroPessoaService.java
│     └─ resources/
│        ├─ Anamnese.fxml
│        ├─ Main.fxml
│        └─ PacienteView.fxml
└─ target/ ... (gerado pelo Maven)
```

---

## Banco de dados
- Driver: `mysql-connector-j`
- Conexão padrão (atual): `jdbc:mysql://localhost:3306/pi_athur`, usuário `root`
- TODOs:
  - Documentar esquema/tabelas e relações esperadas pelo DAO
  - Fornecer scripts SQL de criação e seed de dados
  - Avaliar uso de migrations (Flyway ou Liquibase)

---

## Licença
- TODO: Adicionar arquivo de licença (`LICENSE`) e declarar a licença do projeto neste README.

---

## Segurança e boas práticas
- Evitar manter credenciais no código fonte. Mover para variáveis de ambiente ou arquivos ignorados pelo VCS.
- Validar entradas de usuário (front já possui validações básicas; reforçar no backend/DAO quando aplicável).
- Tratar exceções de acesso a dados com logs adequados (SLF4J) e mensagens amigáveis.

---

## Status e próximos passos (sugestões)
- [ ] Externalizar configuração de banco
- [ ] Adicionar testes (JUnit + Mockito)
- [ ] Documentar DDL e adicionar migrations
- [ ] Empacotamento com JavaFX (jlink/installer) para distribuição
- [ ] Revisar dependências não utilizadas (JPA/Hibernate) ou evoluir o DAO para JPA

---

## Créditos
- Projeto acadêmico/experimental para gestão odontológica.

