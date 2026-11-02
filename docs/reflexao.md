# Reflexão Crítica sobre a Arquitetura do Lisboa Pedala

## 1. Avaliação do Processo Arquitetural

A conceção da arquitetura do *Lisboa Pedala* seguiu uma abordagem orientada pelos atributos de qualidade identificados nos cenários arquiteturais (segurança/conformidade legal, disponibilidade operacional e manutenibilidade).

A decisão de adotar um **monólito modular em Java 21 particionado por domínio** (ADR-001 e ADR-002) revelou-se adequada à dimensão do projeto e da equipa (3 elementos):
* Evitou a complexidade operacional, latência e custo de orquestração de uma arquitetura de microserviços.
* Garantiu fronteiras limpas de domínio entre módulos (`viagens`, `estacoes`, `manutencao`, `faturacao`, `relatorios`), viabilizando a futura extração de componentes caso a escala do sistema o exija.

## 2. Gestão de Trade-offs e Evolução dos ADRs

A evolução dos Registos de Decisão Arquitetural (ADRs) espelha a maturação do desenho:
1. **Governança de Acoplamento (ADR-005 vs ADR-006):**
   * A proposta inicial do ADR-005 gerou um ciclo de dependência bidirecional entre `GestaoViagens` e `Manutencao`.
   * A revisão no **ADR-006** corrigiu esta falha através de comunicação unidirecional baseada em eventos, restabelecendo um grafo estritamente acíclico e permitindo que as *fitness functions* de ArchUnit fossem validadas com sucesso.
2. **Segregação de Dados e Conformidade (ADR-004):**
   * O conflito entre o tempo de retenção fiscal de 10 anos exigido pela AT e a anonimização de percursos imposta pelo RGPD foi resolvido através de esquemas de dados isolados por módulo, sem persistência partilhada.

## 3. Governança Automatizada com ArchUnit

O uso do ArchUnit como teste de arquitetura contínuo provou ser crucial:
* Assegura que o isolamento formal não depende apenas de disciplina humana, mas é verificado automaticamente no processo de *build* Maven (`mvn test`).
* Impede dependências indevidas entre camadas analíticas (`relatorios`) e dados fiscais/faturação (`faturacao`), garantindo *privacy by design*.

## 4. Distribuição de Responsabilidades e Trabalho em Equipa

A segregação dos módulos no ficheiro `CODEOWNERS` permitiu desenvolvimento paralelo sem colisões:
* **US-1 (`@Davidprpinheiro04`):** Ciclo de vida da viagem e orquestração base (`viagens`, `app`).
* **US-2 (`@Brupos`):** Gestão física de postos e integridade de parque (`estacoes`, `manutencao`).
* **US-4 (`@matiasdavo`):** Tarifação, fecho fiscal e métricas municipais (`faturacao`, `relatorios`).
