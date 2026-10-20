# AGENTS_AI.md — Memória do projeto

> Este arquivo é a **memória** do projeto. Qualquer IA ou chat novo deve ler este arquivo
> inteiro antes de fazer qualquer coisa. Sempre atualizar ao final de cada sessão de trabalho.

---

## 1. Sobre o usuário

- Nome: Diego. Idioma: **português**.
- Objetivo: **aprender** (não é projeto de produção). Diz ter deficiências em programação.
- Quer aprender: Java 21, testes, Spring Boot, JWT, Kafka, fila, banco de dados,
  API Gateway, Load Balancer, **DRY, SOLID e boas práticas**.
- Preferências: projeto **simples**, feito **uma etapa de cada vez**.

### Como a IA deve trabalhar com o Diego
- Explicar o **porquê** de cada decisão, não só o código.
- Passos pequenos; ao fim de cada etapa o projeto deve compilar e os testes devem passar.
- Apontar quando um princípio (SOLID/DRY) está sendo aplicado e por quê.
- Deixar o Diego escrever código quando ele quiser; revisar e explicar erros com paciência.
- **IMPORTANTE (2026-10-05): o Diego quer fazer ele mesmo.** A IA deve dar o **passo a passo**
  e explicar; **não criar os projetos/código por ele**, a menos que ele peça explicitamente.

---

## 2. Ambiente (verificado em 2026-10-05)

- macOS. **Pasta raiz: `/Users/diego/projects/flow`**. Ela **NÃO é um projeto**: é a pasta que
  **contém** os projetos (um subdiretório por serviço). Só o `AGENTS_AI.md` fica na raiz.
  Não é repositório git ainda.
- Java instalado: OpenJDK **25** (o projeto vai compilar com `release 21`).
- Maven 3.9.11 instalado. Gradle não instalado → **usar Maven**.
- **MySQL 9.5.0** instalado nativamente (instalador oficial) em `/usr/local/mysql`
  (binários fora do PATH: `/usr/local/mysql/bin/mysql`). **MySQL Workbench** instalado.
  Homebrew instalado.
  - **Ligar o MySQL:** o botão Start do Workbench NÃO funciona (não chega ao servidor).
    Usar no terminal: `sudo /usr/local/mysql/support-files/mysql.server start`
    (`stop` para desligar). Log: `sudo tail -40 /usr/local/mysql/data/*.err`.
    Diagnosticado em 2026-10-05: MySQL saudável, só não estava ligado.
    2026-10-13: Diego ativou o serviço do launchd:
    `sudo launchctl bootstrap system /Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist`
    → MySQL rodando via launchd (state = running, `--user=_mysql`, log em
    `/usr/local/mysql/data/mysqld.local.err`). Plist tem RunAtLoad=true → **deve ligar sozinho**
    após reiniciar o Mac (confirmar no próximo reboot). Não usar mais `mysql.server start`
    junto com o launchd (conflito na porta 3306). Para parar via launchd:
    `sudo launchctl bootout system/com.oracle.oss.mysql.mysqld`.
- **Docker NÃO instalado** → necessário antes da etapa do Kafka
  (alternativas: Docker Desktop, OrbStack, Colima).

---

## 3. A ideia: **SensorFlow** (status: APROVADA em 2026-10-05)

Sistema que recebe **leituras de sensores** (ex.: temperatura de câmaras frias de um mercado).

### Caminho QUENTE (hot path) — tempo real, alto volume
- Sensores enviam muitas leituras por segundo (`POST /readings`).
- Leitura vai para o **Kafka** (tópico `sensor-readings`).
- Um consumidor verifica **na hora** se a temperatura passou do limite → cria um **alerta**.
- Exemplo: câmara de carnes a 9°C (limite 4°C) → alerta imediato.

### Caminho FRIO (cold path) — pode esperar
- As **mesmas leituras do mesmo tópico Kafka** são lidas por um **segundo consumer group**
  (`report-group`), independente do grupo de alertas (`alert-group`).
- Esse consumidor processa com calma, **em lotes**: salva o histórico e calcula **médias por hora**.
- Se ele ficar atrasado ou parar, o caminho quente **não é afetado**: o Kafka guarda as
  mensagens e ele continua de onde parou (offset).

### Desenho geral
```
Cliente/Sensor
     │  (JWT)
     ▼
API Gateway (Spring Cloud Gateway) ── valida JWT
     │  Load Balancer
     ▼
sensor-service (2 instâncias)
     ▼
Kafka (tópico sensor-readings)
     ├──► alert-group  (consumer rápido)  ──► MySQL (alertas)          [QUENTE]
     └──► report-group (consumer em lote) ──► MySQL (histórico/médias) [FRIO]
```

### Stack
Java 21 · Spring Boot 4.1.1 · Maven · **MySQL** · Spring Data JPA · Flyway ·
Spring Security + JWT · Kafka · Spring Cloud Gateway · Spring Cloud LoadBalancer ·
JUnit 5 · Mockito · AssertJ · Testcontainers · Docker Compose.

---

## 4. Roteiro (uma etapa por vez)

| # | Etapa | O que se aprende | Status |
|---|-------|------------------|--------|
| 0 | Criar `flow/sensor-service` (Spring Boot vazio que sobe) | Estrutura Maven, Spring Boot | ✅ |
| 0.5 | Instalar Docker (necessário antes da etapa 5 / Kafka) | — | ⬜ |
| 1 | CRUD de **Sensor** (cadastro) já com **MySQL** local | Camadas Controller/Service/Repository, DTO, validação, **SRP** | ✅ |
| 2 | Testes unitários e de integração da etapa 1 | JUnit 5, Mockito, MockMvc, AssertJ | ✅ |
| 3 | **Flyway** (migrations) no lugar do `ddl-auto` + banco de teste separado | Migrations, profiles | ✅ |
| 4 | Endpoint `POST /readings` + regra de alerta (síncrono) | Regras de negócio, **OCP/DIP** (interfaces) | ⬜ |
| 4.5 | **Simulador de sensores** (`flow/sensor-simulator`): gera leituras falsas e envia muitas por segundo ao `POST /readings` | Cliente HTTP, agendamento, gerar volume | ⬜ |
| 5 | **Caminho quente** com Kafka (`alert-group`) | Producer/consumer, tópicos, partições, assíncrono | ⬜ |
| 6 | **Caminho frio** com Kafka (`report-group`, consumo em lote) | Consumer groups, offsets, batch, retry/DLT | ⬜ |
| 7 | **JWT** (login e proteção de rotas) | Spring Security, tokens | ⬜ |
| 8 | **API Gateway** | Roteamento, validação de JWT na borda | ⬜ |
| 9 | **Load Balancer** (2 instâncias do sensor-service) | Escalabilidade horizontal | ⬜ |
| 10 | Revisão final: DRY, SOLID, refatorações | Boas práticas | ⬜ |
| — | (Opcional, futuro) Trocar o caminho frio para RabbitMQ e comparar | Filas vs log | ⬜ |

Legenda: ⬜ não iniciado · 🟨 em andamento · ✅ concluído

---

## 5. Decisões tomadas

- 2026-10-05: **Não há sensores reais.** O `sensor-service` NÃO mede temperatura: ele é o
  backend que **recebe** leituras, cadastra sensores, gera alertas e relatórios. As leituras
  virão primeiro de `curl`/testes e depois de um projeto **`flow/sensor-simulator`** (etapa 4.5)
  que gera temperaturas falsas em alto volume.
- 2026-10-05: Build com **Maven**. Projeto começa como **um único serviço** (monólito simples);
  só é dividido em serviços nas etapas 8–9, quando o gateway/load balancer fizerem sentido.
- 2026-10-05: **Usar só Kafka por enquanto** (pedido do Diego). RabbitMQ removido da stack.
  Quente e frio = **mesmo tópico** `sensor-readings`, **dois consumer groups**
  (`alert-group` rápido; `report-group` em lote). RabbitMQ fica como etapa opcional futura.
- 2026-10-05: **Spring Boot 4.1.1** (a versão atual; o Spring Initializr não oferece mais a 3.x).
  Atenção: no Boot 4 o starter web se chama `spring-boot-starter-webmvc` e há starters de teste
  separados por módulo (`spring-boot-starter-webmvc-test`, `spring-boot-starter-actuator-test`).
  Material antigo na internet (Boot 3) pode ter nomes diferentes.
- 2026-10-05: **Estrutura de pastas**: `flow/` é a raiz; cada serviço é uma subpasta.
  Primeiro projeto: `flow/sensor-service` (groupId **`com.vanguard`**, artifactId `sensor-service`,
  pacote **`com.vanguard.sensorservice`**). Convenção de pacotes: tudo minúsculo, sem `-`/`_`. Futuros: `flow/gateway`, etc.
  Gerado pelo start.spring.io com dependências `Spring Web` + `Spring Boot Actuator`.
- Possível pegadinha: se o pom vier com `<version>4.1.1.RELEASE</version>` no parent, o Maven
  não encontra; o correto é `4.1.1`.
- 2026-10-05: Configuração em **YAML** (`application.yml`), não `.properties`
  (opção "Configuration: YAML" no start.spring.io).
- 2026-10-05: **Testes manuais de API com Postman** (não curl). Collection `SensorFlow`,
  environment `local` com variável **`baseUrlLocal`** `= http://localhost:8080`. Exportar a collection
  para `flow/postman/` para ficar versionada junto do projeto. Nas instruções, a IA deve
  descrever as requisições no formato Postman (método, URL com `{{baseUrlLocal}}`, body).
- 2026-10-05: **Organização de pacotes POR CAMADA** (escolha do Diego; a IA havia sugerido
  por funcionalidade). Dentro de `com.vanguard.sensorservice`: `controller/`, `service/`,
  `repository/`, `model/` (e depois `dto/`, `exception/`, `config/` conforme surgirem).
  O pacote raiz identifica o projeto; os subpacotes são as camadas.
- 2026-10-05: **DTOs serão `record`s** no pacote `dto/`. Entidades JPA (`model/`) continuam
  sendo **classes** (JPA precisa de construtor vazio e objetos mutáveis; record não serve).
- 2026-10-05: **Sem Lombok** por enquanto (escrever getters/construtores à mão para aprender).
  Entidades **sem setters públicos** desnecessários (encapsulamento).
- 2026-10-05: **Banco = MySQL desde já** (Diego não quer banco em memória). Usa o MySQL 9.5
  **instalado no Mac** (sem Docker por enquanto). PostgreSQL/H2 descartados.
  - Database `sensordb`; usuário dedicado `sensor_app`@`localhost` (não usar root na app).
  - Senha **não vai no código**: `application.yaml` usa **`${SENSOR_DB_PASSWORD}`** (variável de
    ambiente com a senha do `sensor_app`). ⚠️ NÃO usar `DB_PASSWORD`: o Diego já tem uma variável
    `DB_PASSWORD` num arquivo dele com a senha do **root** do MySQL. Convenção: prefixar variáveis
    com o nome do serviço (`SENSOR_...`).
  - Por enquanto `spring.jpa.hibernate.ddl-auto: update`; na Etapa 3 trocar por **Flyway**.
  - `hibernate.jdbc.time_zone: UTC` (Instant salvo em UTC).
  - Consequência: `./mvnw test` (contextLoads) agora **exige o MySQL rodando** e a variável
    SENSOR_DB_PASSWORD definida. Banco de teste separado fica para Etapa 2/3.
- 2026-10-13: Commits feitos pela IA terminam com a linha
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Rodar sempre com o **Maven Wrapper** (`./mvnw`), não com o `mvn` global.

## 6. Estado atual

- 2026-10-05: A IA criou o projeto por engano **direto na raiz `flow/`**. O Diego corrigiu:
  `flow/` é só a pasta raiz. **Tudo foi apagado** (só restou este arquivo).
- A IA passou ao Diego o **passo a passo da Etapa 0** para ele criar `flow/sensor-service`.
- **Etapa 0: em andamento, sendo feita pelo Diego.**
  - Diego gerou `flow/sensor-service` com groupId **`com.vanguard`** (**intencional**, é a
    escolha dele: usar `com.vanguard` em todos os projetos) e pacote `com.vanguard.sensor_service`.
    ⚠️ Renomeação ficou **incompleta**: Diego criou as pastas de camada em
    `com/vanguard/sensorservice/` (controller, service, repository, model, dto, exception), mas
    `SensorServiceApplication` ficou em `com.vanguard` e o teste ficou em `com.vanguard.sensor_service`.
    Testes passam mesmo assim (o @SpringBootTest procura a config "para cima"). Pedido ao Diego
    para mover os dois para `com.vanguard.sensorservice`.
    → Diego moveu a classe principal (ok). O teste ainda está em `sensor_service` e agora
    **falha** ("Unable to find a @SpringBootConfiguration by searching packages upwards").
    Explicado; a pedido do Diego, **a IA moveu o teste** para `com.vanguard.sensorservice`.
    ✅ `./mvnw test` → BUILD SUCCESS. Pacotes corretos.
  - Config: `src/main/resources/application.yaml`.
  - ✅ Passo 4: `./mvnw test` → 1 teste, BUILD SUCCESS. Avisos do Mockito/"Java agent loaded
    dynamically" são inofensivos (Java 25); explicado ao Diego.
  - ✅ Passos 5 e 6: app sobe e `/actuator/health` responde UP. **Etapa 0 concluída.**

## 7. Próximo passo

- **Etapa 2 ✅ concluída (2026-10-13)** — testes **escritos pela IA** a pedido explícito do Diego
  ("pode fazer todos os testes"). **14 testes, BUILD SUCCESS**:
  - `service/SensorServiceTest` (5) — unitário, `@ExtendWith(MockitoExtension.class)`, `@Mock`
    repository + `@InjectMocks` service, `ArgumentCaptor`, `ReflectionTestUtils.setField` p/ id.
  - `controller/SensorControllerTest` (5) — `@WebMvcTest` + MockMvc + `@MockitoBean` service;
    201+Location, 400 (errors.name/maxTemperature, service nunca chamado), 200 lista, 200 id, 404.
  - `repository/SensorRepositoryTest` (3) — `@DataJpaTest` +
    `@AutoConfigureTestDatabase(replace = NONE)` → usa o **MySQL real** (`sensordb`) com
    **rollback** (nada fica gravado; verificado). `TestEntityManager.clear()` p/ ler do banco.
  - `SensorServiceApplicationTests` (1) — contextLoads.
  - Pacotes Boot 4: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
    `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
    `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`,
    `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
    `org.springframework.test.context.bean.override.mockito.MockitoBean`.
  - Limitação: repository test + contextLoads exigem MySQL ligado + SENSOR_DB_PASSWORD
    (resolver na Etapa 3 com banco de teste separado/Testcontainers).
  - Ainda **não commitado**. Próximo: Diego ler/entender os testes; commit "Etapa 2".
- Depois: **Etapa 3** (Flyway + banco de teste separado). Docker ainda não instalado.
- **Pendência adiada pelo Diego (2026-10-13):** trocar a senha do `sensor_app` (apareceu em print
  e está no histórico/log/autosave do Workbench; NÃO está no git). Plano: atualizar
  SENSOR_DB_PASSWORD no ~/.zshrc → `MYSQL_PWD="$DB_PASSWORD" mysql -u root -e "ALTER USER
  'sensor_app'@'localhost' IDENTIFIED BY '$SENSOR_DB_PASSWORD';"` → limpar arquivos do Workbench.
- 2026-10-20: Diego esvaziou a tabela (`TRUNCATE TABLE sensors`) — banco sem dados.
- **Etapa 3 iniciada (2026-10-20)** — Parte A: Flyway. Instruções passadas:
  0. commitar Etapa 2 (testes + memória);
  1. pom: `spring-boot-starter-flyway`, `org.flywaydb:flyway-mysql` (versão vem do Boot: 12.4.0),
     `spring-boot-starter-flyway-test` (test) — validado pela IA (compila);
  2. `DROP TABLE sensors` (tabela vazia, criada pelo Hibernate) para o Flyway criá-la do zero
     (alternativa não usada: `baseline-on-migrate`);
  3. `src/main/resources/db/migration/V1__create_sensors_table.sql`;
  4. yaml `ddl-auto: validate`;
  5. rodar e conferir `flyway_schema_history`.
  Parte B (depois): banco de teste separado.
  Estado em 2026-10-20: Etapa 2 commitada e pushada (`14b9ace`, mensagem saiu literalmente
  `git commit -m "Etapa 2: ..."` — deixada assim, já está no remoto). Diego criou a branch
  **`feature/impementacao-flyway`** (sic) para a Etapa 3. pom com Flyway ✅, `validate` ✅.
  ❌ Pasta criada como `db/migrations` (plural) → Diego corrigiu para `db/migration` ✅.
  Problema seguinte (15:20): V1 foi aplicado às 15:19:39 (criou `sensors`), mas o Diego rodou
  `DROP TABLE sensors` de novo às 15:20:02 → `flyway_schema_history` diz "V1 aplicado" mas a
  tabela não existe → `validate` falha "missing table [sensors]". E o V1 tinha erro: coluna
  `create_at` (deveria ser `created_at`). Solução passada: corrigir V1 + `DROP TABLE
  flyway_schema_history` (reset, ok só em dev local sem dados) + rodar de novo.
  Lição registrada: nunca mexer à mão em tabelas gerenciadas pelo Flyway.
  ✅ **Parte A (Flyway) CONCLUÍDA (2026-10-20)**: V1 corrigido (`created_at`), histórico zerado e
  reaplicado; `flyway_schema_history` com V1 success=1; `ddl-auto: validate` passa; **14 testes
  BUILD SUCCESS** (verificado pela IA). Ainda não commitado (branch `feature/impementacao-flyway`).
  Diego commitou, fez merge na main, pull, e criou a branch **`feature/banco-de-teste-com-fltway`**.
- **Etapa 3 — Parte B (banco de teste separado)** — plano passado em 2026-10-20 (sem Docker ainda):
  1. criar database `sensordb_test` + GRANT para `sensor_app` (via terminal com $DB_PASSWORD);
  2. `src/test/resources/application-test.yaml` sobrescrevendo só `spring.datasource.url`
     → `jdbc:mysql://localhost:3306/sensordb_test` (perfil **test**);
  3. `@ActiveProfiles("test")` em `SensorRepositoryTest` e `SensorServiceApplicationTests`
     (os que usam banco; controller/service não precisam);
  4. rodar testes → Flyway cria `sensors` no `sensordb_test`; dados do `sensordb` intocados.
  Futuro (com Docker): trocar por **Testcontainers** (sem depender do MySQL local).
  ✅ **Parte B CONCLUÍDA (2026-10-20)**: `sensordb_test` criado pelo Workbench (GRANT p/ sensor_app),
  `src/test/resources/application-test.yaml`, `@ActiveProfiles("test")` nos 2 testes com banco.
  Flyway criou `sensors` no `sensordb_test`. Testes passando. **✅ ETAPA 3 CONCLUÍDA.**
  Diego vai commitar/push/PR.
- **Próximo: Etapa 4 — leituras + alertas (síncrono, sem Kafka ainda).** Plano proposto:
  1. `V2__create_readings_table.sql` + entidade `Reading` (id, sensor_id FK, temperature, measured_at);
  2. `POST /readings` (ReadingRequest: sensorId, temperature) → valida sensor existe (404) → salva;
  3. Regra de alerta com interface `AlertRule` (OCP/DIP) — 1ª regra: `MaxTemperatureRule`
     (temperature > maxTemperature); `V3__create_alerts_table.sql` + entidade `Alert`;
  4. `GET /alerts` (e talvez `GET /sensors/{id}/readings`);
  5. testes.

## 8. Diário (log de sessões)

- **2026-10-05** — Primeira conversa. Definidos objetivos de aprendizado, proposta a ideia
  SensorFlow (caminho quente/frio) e o roteiro de 11 etapas. Criado este arquivo.
  Diego perguntou a diferença entre Kafka e RabbitMQ. Explicado com a analogia
  "Kafka = caderno/log que guarda tudo e permite reler; RabbitMQ = carteiro que entrega e a
  mensagem some". Diego decidiu **usar só Kafka por enquanto**; memória atualizada
  (quente/frio = dois consumer groups no mesmo tópico). Ideia ainda aguardando aprovação final.
  Ideia **aprovada**. **Etapa 0 feita**: projeto gerado (Spring Boot 4.1.1, Java 21), testes
  passando, app sobe e responde no `/actuator/health`.
  Diego apontou erro: `flow/` é pasta raiz, não o projeto. Arquivos apagados; agora a IA
  dá o passo a passo e o Diego faz. Primeiro projeto será `flow/sensor-service`.
  Etapa 0 concluída pelo Diego. Ele perguntou se o sensor-service mede temperatura → explicado
  que não; adicionada etapa 4.5 (sensor-simulator).
  Trocado H2 → MySQL local. MySQL estava desligado; Start do Workbench não funcionou; log sem
  erros; ligado com `mysql.server start` (porta 3306 ok). Diego digitou a senha do Mac no chat
  por engano → orientado a trocá-la e a digitar senhas só no terminal.
  ✅ Database `sensordb` + usuário `sensor_app` criados; login do `sensor_app` testado com
  `MYSQL_PWD="$SENSOR_DB_PASSWORD" /usr/local/mysql/bin/mysql -u sensor_app -e "SHOW DATABASES;"`.
  2026-10-07: ✅ pom (JPA, mysql-connector-j, data-jpa-test) e ✅ application.yaml corretos.
  `./mvnw test` conecta no MySQL (BUILD SUCCESS). ❌ `model/Sensor.java` existe mas está **vazia**
  (sem @Entity/campos) → tabela ainda não criada. Próximo: Diego completar a entidade.
  2026-10-07: ✅ **Entidade `Sensor` completa** (revisada pela IA, igual ao proposto). `./mvnw test`
  → BUILD SUCCESS e Hibernate criou `sensors` no MySQL (id bigint auto_increment, created_at
  datetime(6), max_temperature float(53), name varchar(100)).
  ✅ **Passo 2 (Repository)**: `repository/SensorRepository extends JpaRepository<Sensor, Long>`
  + `spring.jpa.open-in-view: false`. Verificado: "Found 1 JPA repository interface", BUILD SUCCESS.
  **Passo 3 (DTOs + Service)** — instruções passadas (código validado pela IA numa cópia):
  pom + `spring-boot-starter-validation`; `dto/SensorRequest` (record, @NotBlank @Size(max=100)
  name, @NotNull Double maxTemperature); `dto/SensorResponse` (record id,name,maxTemperature,
  createdAt + `static from(Sensor)`); `exception/SensorNotFoundException` (RuntimeException);
  `service/SensorService` (@Service, injeção por construtor, `create` @Transactional,
  `findAll`/`findById` @Transactional(readOnly=true), findById lança SensorNotFoundException).
  Service é classe concreta (sem interface) por enquanto — YAGNI.
  2026-10-07: ✅ Passo 3 feito pelo Diego (BUILD SUCCESS, 7 classes). Revisão da IA:
  `SensorService.sensorRepository` sem `final`; `SensorNotFoundException(long id)` com primitivo
  (sugerido `Long`); mensagem da exceção em inglês (ok, escolha dele); formatação (Cmd+Option+L).
  **Passo 4 = Controller** (`controller/SensorController`: POST /sensors → 201 + Location,
  GET /sensors, GET /sensors/{id}; `@Valid @RequestBody`). Validado pela IA numa cópia.
  Comportamento esperado ANTES do tratamento de erros: id inexistente → **500** (feio, de
  propósito, para mostrar o problema); body inválido → 400 sem detalhes.
  2026-10-07: ✅ Passo 4 feito: Diego testou no Postman (POST/GET/GET id), **7 sensores** salvos
  no MySQL. `final` corrigido no Service; `SensorNotFoundException(long)` ainda com primitivo.
  **Passo 5 = `exception/GlobalExceptionHandler`** (`@RestControllerAdvice`, retorna
  `ProblemDetail` RFC 9457): SensorNotFoundException → 404; MethodArgumentNotValidException → 400
  com propriedade `errors` {campo: mensagem}. Validado pela IA numa cópia. Mensagens de validação
  saem em português (locale da JVM). Ao concluir → Etapa 1 completa.
  2026-10-13: ✅ Passo 5 feito (handler igual ao proposto; `SensorNotFoundException(Long)` corrigido).
  **✅ ETAPA 1 CONCLUÍDA.** Endpoints: POST /sensors (201), GET /sensors, GET /sensors/{id} (404).
  (Não implementados ainda: PUT/DELETE — podem entrar depois se o Diego quiser.)
  Obs. p/ IA: `SENSOR_DB_PASSWORD` está no `~/.zshrc` do Diego; o shell da ferramenta Bash não
  carrega o .zshrc → rodar testes com `zsh -c 'source ~/.zshrc >/dev/null 2>&1; ./mvnw test'`.
- **2026-10-13** — Mac reiniciado → MySQL parado → Workbench "Could not connect". Orientado a
  ligar com `mysql.server start`. ⚠️ Print do Workbench mostrava a senha do `sensor_app` no
  editor SQL (linha do CREATE USER) → orientado a trocar a senha (ALTER USER + atualizar
  `SENSOR_DB_PASSWORD` no ~/.zshrc) e apagar o SQL com senha da aba de Query.
