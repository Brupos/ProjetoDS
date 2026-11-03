# ARCHITECTURE.md — Lisboa Pedala

## 1. Visão geral

O Lisboa Pedala é desenvolvido como um **monólito modular**, utilizando Java 21, Maven e ArchUnit. A aplicação é executada como uma única unidade de deployment, mas o código está dividido em módulos organizados por domínio de negócio.

Cada módulo tem uma responsabilidade própria e só pode depender dos módulos explicitamente autorizados. A arquitetura deve evitar dependências circulares, partilha indevida de dados e acoplamento desnecessário entre funcionalidades.

**Tecnologias:** Java 21, Maven, JUnit 5 e ArchUnit.

**ADRs de referência:** ADR-001 (estilo de deployment e tecnologia), ADR-002 (particionamento por domínio).

## 2. Componentes e responsabilidades

| Componente | Pacote Java | Responsabilidade |
|---|---|---|
| GestaoViagens | `pt.ulisboa.pedala.viagens` | Gere o ciclo de vida das viagens, incluindo autorização, início, fim e duração. |
| EstacoesDocas | `pt.ulisboa.pedala.estacoes` | Gere o estado das estações, bicicletas, docas e operações de desbloqueio e devolução. |
| Manutencao | `pt.ulisboa.pedala.manutencao` | Processa alertas de avaria e gere ordens de reparação e bloqueios preventivos. |
| TarifarioCobranca | `pt.ulisboa.pedala.faturacao` | Aplica regras tarifárias, processa cobranças e gere a emissão e retenção de faturas. |
| RelatoriosMunicipais | `pt.ulisboa.pedala.relatorios` | Produz indicadores agregados de utilização para consulta municipal. |
| Aplicação | `pt.ulisboa.pedala.app` | Inicializa a aplicação, configura as dependências e liga os módulos. Não contém lógica de negócio. |

**Princípio:** cada componente deve manter a sua lógica e os seus dados dentro do respetivo domínio. Não devem ser criados módulos organizados apenas em torno de entidades isoladas, como `Bicicleta` ou `Utilizador`.

Referências: ADR-002 e ADR-004.

## 3. Regras de dependência (allowed-to-use)

As seguintes regras são obrigatórias e devem ser verificáveis através das fitness functions em `tests/architecture/`.

| Módulo de origem | Pode depender de |
|---|---|
| `viagens` | `estacoes` |
| `estacoes` | Nenhum outro módulo de negócio |
| `manutencao` | `viagens`, `estacoes` |
| `faturacao` | `viagens` |
| `relatorios` | `viagens`, `estacoes` |
| `app` | Módulos necessários à configuração e integração da aplicação |

Regras adicionais:

- É proibido criar dependências circulares entre módulos.
- Nenhum módulo de negócio pode depender de `app`.
- `viagens` não pode depender diretamente de `faturacao` ou `manutencao`.
- `relatorios` não pode depender de `faturacao` nem aceder aos seus dados.
- Os módulos não podem aceder diretamente aos repositórios ou esquemas de persistência uns dos outros.
- Não deve ser criado um pacote `common` para partilhar entidades ou lógica de domínio.

A aplicação deve respeitar a matriz definida no ADR-006. A integração entre módulos deve recorrer a interfaces, contratos públicos e eventos, sem contornar as fronteiras estabelecidas.

## 4. Características arquiteturais condutoras

As três características prioritárias são:

### 4.1 Disponibilidade e resiliência

O sistema deve limitar o impacto de falhas de bicicletas, docas ou serviços periféricos.

Implicações para o código:
- Uma bicicleta ou doca indisponível não deve impedir a utilização das restantes.
- O tratamento de avarias não deve bloquear desnecessariamente o fluxo normal de viagens.
- Os alertas de manutenção devem ser processados de forma desacoplada.
- As falhas de faturação ou de serviços externos não devem impedir a devolução de uma bicicleta.

### 4.2 Elasticidade

O sistema deve suportar picos de procura, nomeadamente aumentos de quatro vezes nos pedidos de desbloqueio durante eventos municipais.

Implicações para o código:
- O caminho crítico de início e fim de viagem deve ser simples e eficiente.
- O processamento de telemetria e tarefas secundárias não deve competir desnecessariamente com os desbloqueios.
- Operações que não necessitam de resposta imediata devem ser executadas em background.
- Evitar processamento pesado ou chamadas externas no caminho crítico.

### 4.3 Segurança e conformidade legal

O sistema deve respeitar o RGPD e os requisitos fiscais da Autoridade Tributária.

Implicações para o código:
- Os dados fiscais e os dados de percurso devem permanecer separados.
- Os eventos de viagem não devem transportar coordenadas ou percursos completos.
- Os relatórios municipais devem utilizar dados agregados e não identificáveis.
- O módulo `relatorios` não pode aceder a dados fiscais ou identificadores pessoais do módulo `faturacao`.
- As faturas devem ser geridas de acordo com os requisitos de retenção fiscal de 10 anos.

Referências: análise de características arquiteturais, ADR-001, ADR-003 e ADR-004.

## 5. Acoplamento temporal e ordem das operações

As operações devem respeitar a seguinte sequência:

### Início de viagem

1. O utilizador solicita o início da viagem.
2. `GestaoViagens` valida a autorização do utilizador.
3. `GestaoViagens` solicita a `EstacoesDocas` a disponibilidade da bicicleta e o desbloqueio da doca.
4. A viagem só deve ser iniciada após a confirmação da operação na doca.
5. O estado da bicicleta e o registo da viagem devem permanecer coerentes.

A autorização e o desbloqueio são operações síncronas, uma vez que o utilizador aguarda uma resposta imediata.

### Fim de viagem

1. O utilizador devolve a bicicleta numa doca.
2. `EstacoesDocas` confirma a devolução e o bloqueio.
3. `GestaoViagens` regista a conclusão da viagem e calcula a duração.
4. Após o registo da conclusão, é publicado o evento `ViagemConcluida`.
5. `faturacao` consome o evento e processa a tarifa e a faturação de forma assíncrona.

A devolução não deve ficar à espera da faturação ou da comunicação com a Autoridade Tributária.

### Alertas de avaria

1. É detetada ou reportada uma avaria.
2. É publicado um evento de alerta.
3. `manutencao` processa o alerta e cria a ordem de reparação.
4. Quando necessário, `manutencao` solicita a `estacoes` o bloqueio preventivo do equipamento.

O processamento da manutenção não deve bloquear desnecessariamente o fluxo de viagens.

**Regra geral:** utilizar chamadas síncronas em memória para operações que exigem confirmação imediata e eventos assíncronos em memória para notificações e processamento secundário. Os eventos devem ser publicados após a confirmação da operação que lhes dá origem.

Referências: ADR-003 e ADR-006.

## 6. Convenções de implementação

- Utilizar Java 21 e manter a organização por pacotes de domínio.
- Utilizar `record` para contratos de dados imutáveis, sempre que adequado.
- Definir interfaces para comunicação entre componentes quando necessário.
- Manter implementações concretas e detalhes de persistência encapsulados nos respetivos módulos.
- Utilizar identificadores opacos nas comunicações entre módulos.
- Normalizar os carimbos temporais em UTC.
- Utilizar nomes de classes e métodos claros, em português, seguindo a convenção existente no projeto.
- Manter testes unitários para o comportamento dos serviços e testes de arquitetura em `tests/architecture/`.
- Garantir que `mvn test` executa os testes e as fitness functions.

## 7. Restrições para código gerado por IA

A IA deve respeitar obrigatoriamente as seguintes regras:

1. Não introduzir dependências fora da matriz `allowed-to-use`.
2. Não criar ciclos de dependência entre módulos.
3. Não criar pacotes `common` ou módulos baseados exclusivamente em entidades.
4. Não aceder diretamente aos dados ou repositórios de outros módulos.
5. Não partilhar entidades de persistência entre módulos.
6. Não introduzir dependências diretas entre `viagens` e `faturacao` ou `manutencao`.
7. Não incluir percursos ou coordenadas nos eventos enviados para faturação.
8. Não colocar chamadas à Autoridade Tributária ou ao sistema de pagamentos no caminho crítico de devolução.
9. Não alterar contratos públicos ou decisões arquiteturais sem justificar a alteração e propor a revisão do ADR correspondente.
10. Não substituir requisitos funcionais por valores, regras ou comportamentos inventados.

Antes de aceitar código gerado, devem ser verificados os requisitos funcionais, os testes e as regras arquiteturais. A aprovação na compilação não substitui a revisão humana.

## 8. ADRs de referência

| ADR | Decisão |
|---|---|
| ADR-001 | Monólito como unidade de deployment e utilização de Java 21, Maven e ArchUnit. |
| ADR-002 | Particionamento estrutural por domínio e prevenção da entity trap. |
| ADR-003 | Chamadas síncronas para operações críticas e eventos assíncronos após commit. |
| ADR-004 | Propriedade e isolamento dos dados por módulo. |
| ADR-005 | Decisão inicial de métricas de acoplamento, posteriormente substituída. |
| ADR-006 | Revisão das métricas e matriz final de dependências permitidas. |

O ADR-006 substitui o ADR-005 e representa a decisão atual sobre o acoplamento entre módulos.
