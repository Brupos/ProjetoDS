# Registo Geral de Utilização de IA

## 1. Ferramentas utilizadas

- **Ferramenta:** ChatGPT (OpenAI)
- **Modelo:** GPT-6
- **Linguagem de programação:** Java 21
- **Utilização:** Análise da especificação, geração de código, criação de testes, revisão e correção de implementações.

## 2. Contexto geral fornecido

A geração dos componentes teve como referência a documentação e as decisões arquiteturais do projeto Lisboa Pedala:

- `ARCHITECTURE.md` — responsabilidades dos componentes, regras `allowed-to-use`, características arquiteturais e convenções de código.
- `docs/01-caracteristicas.md` — características arquiteturais e respetivos cenários.
- `docs/02-componentes.md` — decomposição dos componentes lógicos e relações entre eles.
- ADR-001 — Monólito modular e tecnologias.
- ADR-002 — Particionamento por domínio.
- ADR-003 — Comunicação síncrona e assíncrona entre componentes.
- ADR-004 — Isolamento e propriedade dos dados.
- ADR-006 — Regras de dependência entre componentes.

Foram também utilizados ficheiros Java existentes nos módulos relevantes para garantir a integração com o código desenvolvido.

## 3. Prompts de geração por User Story

### 3.1. US-1 — Gestão de Viagens e Faturação

**Objetivo:** Implementar o fluxo de utilização de bicicletas, desde a autorização e desbloqueio até à devolução, registo da viagem e faturação.

**Pedido feito à IA:**

Implementa os componentes necessários para suportar a US-1 do Lisboa Pedala. O sistema deve permitir validar a autorização do utilizador, iniciar uma viagem através do desbloqueio de uma bicicleta, registar a viagem e permitir a sua conclusão após a devolução da bicicleta. A conclusão deve gerar a informação necessária para o processamento da faturação. Respeita as responsabilidades dos módulos `viagens`, `estacoes` e `faturacao`, as regras de dependência e os contratos definidos na arquitetura.

**Ficheiros de contexto:** `ARCHITECTURE.md`, ADR-002, ADR-003, ADR-004, ADR-006, especificação funcional da US-1, regras tarifárias e classes existentes nos módulos `viagens`, `estacoes` e `faturacao`.

**Resultado:** Implementação aceite após revisão e correções. A violação identificada na regra dos minutos gratuitos está documentada em `violacoes.md`.

### 3.2. US-2 — Manutenção

**Objetivo:** Implementar o processamento de avarias, bloqueio preventivo de bicicletas e criação de ordens de reparação.

**Pedido feito à IA:**

Implementa os componentes necessários para suportar a US-2 do Lisboa Pedala. O sistema deve permitir processar reportes de avarias, identificar bicicletas que necessitam de intervenção, bloquear preventivamente os equipamentos afetados e criar ordens de reparação para a equipa técnica. A comunicação entre módulos deve respeitar os eventos e as regras de dependência definidos na arquitetura.

**Ficheiros de contexto:** `ARCHITECTURE.md`, ADR-002, ADR-003, ADR-004, ADR-006, especificação funcional da US-2 e classes existentes nos módulos `manutencao` e `estacoes`.

**Resultado:** Implementação aceite após revisão e correções. As violações identificadas encontram-se documentadas em `violacoes.md`.

### 3.3. US-4 — Relatórios Municipais

**Objetivo:** Implementar a geração de relatórios de utilização por freguesia e período horário.

**Pedido feito à IA:**

Implementa os componentes necessários para suportar a US-4 do Lisboa Pedala. O sistema deve permitir consultar indicadores de utilização das bicicletas por freguesia e período horário, recorrendo a dados agregados. A implementação deve respeitar o isolamento dos dados entre módulos e garantir que os relatórios não expõem identificadores pessoais nem informação fiscal dos utilizadores.

**Ficheiros de contexto:** `ARCHITECTURE.md`, ADR-002, ADR-004, ADR-006, especificação funcional da US-4 e classes existentes nos módulos `relatorios` e `viagens`.

**Resultado:** Implementação aceite após revisão.

## 4. Revisão e validação

O código gerado foi revisto antes da integração no projeto, considerando os requisitos funcionais, as responsabilidades dos componentes e as regras arquiteturais.

Foram utilizados testes funcionais e fitness functions com ArchUnit para verificar o comportamento e as dependências entre módulos.

As violações identificadas durante o desenvolvimento encontram-se descritas em `violacoes.md`, incluindo o resultado inicial, o método de deteção e a correção aplicada.

A IA foi utilizada como ferramenta de apoio à implementação, mantendo-se a responsabilidade pela revisão, validação e integração do código no grupo.
