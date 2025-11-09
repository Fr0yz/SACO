# Documentação do Sistema SACO

Esta documentação descreve a arquitetura, os principais fluxos de tela, a estrutura do banco de dados e orientações de desenvolvimento/manutenção do **Sistema de Apoio Clínico Odontológico (SACO)**. O texto complementa o `README.md` raiz, fornecendo um panorama detalhado da aplicação JavaFX.

---

## 1. Arquitetura geral

### Camadas e pacotes

A aplicação segue uma divisão em camadas simples, organizada em pacotes no diretório `src/main/java`:

| Pacote            | Responsabilidade principal | Classes de destaque |
|-------------------|-----------------------------|---------------------|
| `controller`      | Lógica das telas JavaFX. Recebe eventos da UI, coordena validações e chama os serviços. | `MainController`, `PacienteController`, `AnamneseController`, `MaterialController` |
| `service`         | Regras de negócio e orquestração de DAOs. Concentra validações adicionais antes da persistência. | `CadastroPessoaService`, `AnamneseService`, `MaterialService` |
| `dao`             | Acesso direto ao banco via JDBC. Cada DAO encapsula SQL específico de uma entidade/tabela. | `CadastroPessoaDao`, `AnamneseDao`, `MaterialDAO`, `Conexao` |
| `model`           | Objetos de domínio (POJOs/Lombok) usados para trafegar dados entre camadas. | `Pessoa`, `Dentista`, `Paciente`, `Anamnese`, `Material` |

A classe `Main` (`src/main/java/Main.java`) é o ponto de entrada. Ela inicializa o JavaFX, carrega `Main.fxml` e exibe a janela principal do sistema.

### Recursos FXML e layout

Os arquivos FXML residem em `src/main/resources`:

- `Main.fxml`: Shell da aplicação, com menu lateral e área central (`StackPane`) controlada por `MainController`.
- `PacienteView.fxml`: Formulário de cadastro/edição de pessoas (pacientes e dentistas) + tabela de listagem. Controlado por `PacienteController`.
- `Anamnese.fxml`: Tela de anamnese vinculada a pacientes, com upload de odontograma e grade de registros. Controlada por `AnamneseController`.
- `Material.fxml`: Tela de controle de materiais/estoque odontológico, com filtros e ajustes rápidos. Controlada por `MaterialController`.

`MainController` carrega dinamicamente os FXML conforme a navegação (cache básico para reaproveitar nós). 【F:src/main/java/controller/MainController.java†L17-L58】

---

## 2. Fluxos principais da aplicação

### 2.1 Cadastro de Pacientes e Dentistas

1. A tela inicial (`PacienteView.fxml`) permite cadastrar pessoas como paciente ou dentista.
2. O usuário preenche nome, CPF, telefone, e-mail e data de nascimento. Ao marcar "É dentista?", campos adicionais (CRO e especialidade) são exibidos dinamicamente. 【F:src/main/java/controller/PacienteController.java†L29-L123】
3. O botão **Salvar** chama `CadastroPessoaService`, que valida os campos (ex.: CPF com 11 dígitos, obrigatoriedade de CRO para dentistas) antes de delegar ao DAO. 【F:src/main/java/service/CadastroPessoaService.java†L24-L73】
4. Em `CadastroPessoaDao` a inserção ocorre em transação: primeiro em `TB_PESSOA` e, de acordo com o tipo, em `TB_DENTISTA` ou `TB_PACIENTE`. O mesmo DAO lida com atualização, exclusão e listagem. 【F:src/main/java/dao/CadastroPessoaDao.java†L11-L205】
5. A tabela apresenta máscaras de CPF/telefone e oferece ações para editar ou remover registros individuais. 【F:src/main/java/controller/PacienteController.java†L67-L226】

### 2.2 Anamnese e Odontograma

1. A tela `Anamnese.fxml` carrega a lista de pacientes por meio de `CadastroPessoaService.listarPacientes()`. 【F:src/main/java/controller/AnamneseController.java†L64-L105】
2. Ao selecionar um paciente, `AnamneseService` busca (via `AnamneseDao`) o registro de anamnese existente, incluindo os bytes do odontograma (se houver). 【F:src/main/java/controller/AnamneseController.java†L129-L168】【F:src/main/java/dao/AnamneseDao.java†L20-L87】
3. O usuário pode preencher alergias, histórico médico, medicamentos, detalhes e anexar uma imagem de odontograma (armazenada como `LONGBLOB`).
4. A ação de salvar chama `AnamneseService.salvarOuAtualizar`, que executa uma transação completa: upsert da anamnese, upsert/remoção da imagem e atualização automática do timestamp. 【F:src/main/java/service/AnamneseService.java†L10-L33】【F:src/main/java/dao/AnamneseDao.java†L207-L302】
5. A tabela à direita mostra data e resumos dos campos textuais, além de indicar se existe odontograma anexado. 【F:src/main/java/controller/AnamneseController.java†L108-L125】

### 2.3 Gestão de Materiais

1. `Material.fxml` apresenta formulário simples e tabela com filtro por nome. 【F:src/main/resources/Material.fxml†L9-L46】
2. `MaterialController` coordena o CRUD e aciona `MaterialService`, que encapsula regras como impedir quantidades negativas, ajustes incrementais e diálogos de confirmação. 【F:src/main/java/controller/MaterialController.java†L18-L213】【F:src/main/java/service/MaterialService.java†L11-L101】
3. `MaterialDAO` executa as operações SQL na tabela `TB_MATERIAL`. 【F:src/main/java/dao/MaterialDAO.java†L11-L88】

---

## 3. Estrutura do banco de dados

A persistência utiliza MySQL com acesso JDBC. As tabelas esperadas pelo código (inferidas dos DAOs) são:

```sql
CREATE TABLE TB_PESSOA (
    ID_PESSOA      INT AUTO_INCREMENT PRIMARY KEY,
    NOME           VARCHAR(120) NOT NULL,
    CPF            VARCHAR(11)  NOT NULL UNIQUE,
    TELEFONE       VARCHAR(11),
    EMAIL          VARCHAR(120),
    DT_NASCIMENTO  DATE
);

CREATE TABLE TB_DENTISTA (
    ID_DENTISTA   INT PRIMARY KEY,
    CRO           VARCHAR(30)  NOT NULL,
    ESPECIALIDADE VARCHAR(120) NOT NULL,
    CONSTRAINT FK_DENTISTA_PESSOA
        FOREIGN KEY (ID_DENTISTA) REFERENCES TB_PESSOA(ID_PESSOA)
        ON DELETE CASCADE
);

CREATE TABLE TB_PACIENTE (
    ID_PACIENTE INT PRIMARY KEY,
    CONSTRAINT FK_PACIENTE_PESSOA
        FOREIGN KEY (ID_PACIENTE) REFERENCES TB_PESSOA(ID_PESSOA)
        ON DELETE CASCADE
);

CREATE TABLE TB_ANAMNESE (
    ID_ANAMNESE      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ID_PACIENTE      INT        NOT NULL,
    ALERGIAS         TEXT,
    HISTORICO_MEDICO TEXT,
    MEDICAMENTOS     TEXT,
    DETALHES         TEXT,
    DATA_REGISTRO    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_ANAMNESE_PACIENTE
        FOREIGN KEY (ID_PACIENTE) REFERENCES TB_PACIENTE(ID_PACIENTE)
        ON DELETE CASCADE
);

CREATE TABLE TB_ODONTOGRAMA (
    ID_ODONTOGRAMA BIGINT AUTO_INCREMENT PRIMARY KEY,
    ID_PACIENTE    INT      NOT NULL,
    IMAGEM_REF     LONGBLOB,
    DATA_CRIACAO   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_ODONTO_PACIENTE
        FOREIGN KEY (ID_PACIENTE) REFERENCES TB_PACIENTE(ID_PACIENTE)
        ON DELETE CASCADE
);

CREATE TABLE TB_MATERIAL (
    ID_MATERIAL INT AUTO_INCREMENT PRIMARY KEY,
    NOME        VARCHAR(120) NOT NULL,
    QUANTIDADE  INT NOT NULL DEFAULT 0
);
```

> **Observações:**
> - Os DAOs assumem `ON DELETE CASCADE` em todas as FKs para garantir remoção em cadeia (ver `CadastroPessoaDao.excluirPessoa`).
> - Ajuste tamanhos/constraints conforme as regras da clínica (por exemplo, aumentar limite de caracteres de CRO/especialidade).
> - Caso utilize outro SGBD, adapte os tipos (`DATETIME`, `LONGBLOB`, auto incremento) e comandos `NOW()` utilizados nos DAOs.

### Conexão com o banco

As credenciais estão atualmente hardcoded em `dao.Conexao`:

```java
private static final String URL = "jdbc:mysql://localhost:3306/pi_athur";
private static final String USER = "root";
private static final String PASSWORD = "iarc1001";
```

Recomenda-se externalizar esses valores (variáveis de ambiente ou arquivo de configuração). 【F:src/main/java/dao/Conexao.java†L9-L36】

---

## 4. Guia de desenvolvimento

### 4.1 Pré-requisitos

- JDK 17 configurado no ambiente.
- Maven 3.8+ para build/execução (`mvn clean javafx:run`).
- Servidor MySQL acessível com o schema `pi_athur` (ou configure outro URL na classe `Conexao`).

### 4.2 Convenções de código

- Controllers usam anotações `@FXML` e devem manter campos privados, inicializados via injeção do JavaFX.
- Services encapsulam validações e lançam exceções específicas (por exemplo, `MaterialService.ServiceException`).
- DAOs retornam POJOs simples (`model.*`) e sempre fecham recursos usando `try-with-resources` ou blocos `finally`.
- Máscaras e formatações de CPF/telefone estão implementadas diretamente no controller (`PacienteController`) para feedback imediato ao usuário.

### 4.3 Boas práticas e TODOs

- **Configuração sensível:** mover credenciais de banco para um local seguro e adicionar suporte a variáveis de ambiente.
- **Scripts SQL:** transformar o DDL acima em scripts versionados (Flyway/Liquibase) para facilitar deploy.
- **Testes automatizados:** inexistentes no momento; considere adicionar testes de serviço/DAO com um banco em memória ou containerizado.
- **Validações adicionais:** implementar validação formal de CPF/CRO e regras de negócio específicas da clínica.
- **Tratamento de erros na UI:** substituir `printStackTrace()` por logs (`SLF4J`) e mensagens mais amigáveis.

---

## 5. Passos comuns de operação

1. **Compilar/executar**
   ```bash
   mvn clean javafx:run
   ```
2. **Empacotar JAR**
   ```bash
   mvn clean package
   ```
3. **Resetar banco (ambiente de testes)**
   - Rodar os scripts DDL acima.
   - Popular as tabelas com dados iniciais conforme a necessidade (por exemplo, pacientes/dentistas fictícios).

---

## 6. Referências rápidas

- `MainController` controla a navegação entre telas via `StackPane`. 【F:src/main/java/controller/MainController.java†L17-L58】
- `PacienteController` trata máscaras de CPF/telefone e integra com `CadastroPessoaService`. 【F:src/main/java/controller/PacienteController.java†L62-L230】
- `AnamneseController` manipula upload de imagem e resume textos na tabela. 【F:src/main/java/controller/AnamneseController.java†L32-L172】
- `MaterialController` implementa ações de estoque com diálogos e filtros locais. 【F:src/main/java/controller/MaterialController.java†L18-L213】
- DAOs correspondentes contêm o SQL de cada tabela (`dao/*.java`).

---

## 7. Glossário rápido

- **Paciente**: registro básico em `TB_PESSOA` com vínculo obrigatório em `TB_PACIENTE`.
- **Dentista**: pessoa com campos adicionais (`CRO`, `Especialidade`) na tabela `TB_DENTISTA`.
- **Anamnese**: formulário clínico por paciente; somente um registro ativo por pessoa (atualizado via upsert).
- **Odontograma**: imagem opcional associada à anamnese, armazenada como blob.
- **Material**: item de estoque (consumível/equipamento), com controle de quantidade inteira.

Esta documentação deve servir como ponto de partida para novos contribuidores e para manutenção contínua do SACO.
