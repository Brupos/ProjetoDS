# ProjetoDS

## O que Temos de Fazer

### 1. Documentação de Arquitetura (Pasta `docs/`)
- [ ] **`01-caracteristicas.md`**:
  - Worksheet com até 7 características candidatas (implícitas numa coluna à parte e ligadas às frases do enunciado).
  - Seleção e destaque das **Top 3** características (sem ordenar).
  - Pelo menos 1 característica composta decomposta.
  - 1 cenário concreto para cada uma das Top 3 no contexto do Lisboa Pedala.
- [ ] **`02-componentes.md`**:
  - Definir componentes lógicos (escolher abordagem: *workflow* ou *actor/action*).
  - Mapear User Stories → Componentes (evitando a *entity trap*).
  - Análise de acoplamento aferente ($Ca$), eferente ($Ce$) e instabilidade ($I$) por componente.
  - Exemplo de conascência entre pelo menos 2 componentes.
  - 2 exemplos de acoplamento temporal.
  - Registar pelo menos 1 iteração de reestruturação com o respetivo motivo.
- [ ] **`03-particionamento-e-vistas.md`**:
  - Escolha do particionamento de topo (técnico vs. domínio) com trade-offs explícitos.
  - **Vista de Módulos:** Regras *allowed-to-use*.
  - **Vista C&C (Componentes e Conectores):** Fluxo *desbloquear → devolver → cobrar*.
  - **Vista de Alocação:** Onde corre cada parte e atribuição de responsabilidades aos membros (`CODEOWNERS`).
- [ ] **`docs/reflexao.md`** (Máximo 2 páginas):
  - O que a especificação não previu.
  - Que violações arquiteturais a IA cometeu, quais as fitness functions apanharam e quais passaram despercebidas.

---

### 2. Decisões Arquiteturais e Diagramas (`adr/` e `diagramas/`)
- [ ] **ADRs (`adr/`):**
  - Mínimo de 5 ADRs com: *Contexto*, *Decisão*, *Consequências* e *Alternativas Rejeitadas*.
  - Pelo menos **1 ADR revisto ou substituído** ao longo do projeto.
- [ ] **Diagramas C4 (`diagramas/`):**
  - Níveis 1 (Contexto), 2 (Contentores) e 3 (Componentes).
  - Criados em Mermaid, PlantUML ou Structurizr DSL com legenda e notação uniforme.

---

### 3. Ficheiro `ARCHITECTURE.md` (Guia para IA)
- [ ] Documento conciso (máx. 4 páginas) para guiar a IA antes da geração de código:
  - Lista de componentes, responsabilidade em 1 frase e namespace/pacote.
  - Regras *allowed-to-use* verificáveis (não apenas sugestões).
  - Top 3 características e implicações diretas no código.
  - Acoplamento temporal (ordem de operações / transações).
  - Convenções de código e restrições explícitas do que a IA **não pode fazer**.
  - Referência aos ADRs aplicáveis por componente.

---

### 4. Implementação e Código (`src/` e `tests/`)
- [ ] **Tecnologia:** Python 3.11+ ou Java 21.
- [ ] **Esqueleto:** Todos os componentes lógicos criados com base no particionamento escolhido.
- [ ] **3 User Stories implementadas end-to-end:**
  - **US-1 (Obrigatória):** Como assinante, desbloqueio e devolvo uma bicicleta e a viagem fica registada e cobrada.
  - **US Escolhida 2:** (Escolher entre US-2 manutenção, US-3 doca offline, ou US-4 relatório de freguesias).
  - **US Escolhida 3:** (Pelo menos uma US do projeto tem de envolver 3+ componentes).
- [ ] **Fitness Functions (`tests/architecture/`):**
  - Testes automáticos em 1 comando usando `import-linter` (Python) ou `ArchUnit` (Java).
  - Devem validar as regras *allowed-to-use* e a ausência de ciclos de dependência.
- [ ] Persistência simples (SQLite ou em memória) e simulação de hardware/docas/pagamentos (REST ou CLI).

---

### 5. Registo de Uso de IA (`prompts/`)
- [ ] Criar 1 ficheiro por componente gerado em `prompts/`:
  - Pedido e ficheiros de contexto passados.
  - Resultado aceite ou rejeitado (e porquê).
  - Violações detetadas e por quem (fitness function, revisão humana, etc.).
- [ ] **Obrigatório:** Documentar pelo menos **3 casos** em que o código gerado violou a especificação.
- [ ] **Preparação para o Teste de Regeneração:** Estar pronto para em aula apagar 1 componente e regenerá-lo com IA em 30 min (tem de passar nos testes e fitness functions à primeira ou segunda tentativa).

---

