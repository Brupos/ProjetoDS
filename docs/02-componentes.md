# 02 - Decomposição de Componentes Lógicos (Lisboa Pedala)

## 1. Abordagem de Identificação de Componentes

Para a identificação dos componentes lógicos adotou-se a **Workflow Approach** (abordagem orientada ao fluxo de trabalho), complementada pontualmente pela perspetiva de atores nas interações periféricas (técnicos de manutenção e gestores municipais).

Em vez de modelar componentes a partir dos dados persistidos (o que induziria a *entity trap*), as fronteiras foram traçadas com base nos processos de negócio do sistema:
* O ciclo de vida operacional de uma viagem (do desbloqueio à devolução).
* A gestão do inventário e controlo de trincos físicos em estações e docas.
* A monitorização do estado da frota e triagem de avarias.
* O cálculo tarifário e processamento fiscal de faturas.
* A agregação e produção de métricas públicas e municipais.

---

## 2. Componentes Lógicos Identificados (Prevenção da *Entity Trap*)

Para evitar a *entity trap* (definir componentes que espelham diretamente tabelas da base de dados como `Bicicleta`, `Estacao` ou `Utilizador`), as responsabilidades foram agrupadas por capacidades funcionais coesas:

| Componente Lógico | Pacote Java | Responsabilidade Nuclear | O que **não** faz (Prevenção da *Entity Trap*) |
| :--- | :--- | :--- | :--- |
| **`GestaoViagens`** | `pt.ulisboa.pedala.viagens` | Coordena o ciclo de vida completo de cada viagem: valida autorização de início, regista início/fim de aluguer, processa telemetria em andamento e calcula a duração de utilização. | Não calcula preços detalhados nem taxas, não emite faturas e não atua diretamente nos trincos mecânicos das docas. |
| **`EstacoesDocas`** | `pt.ulisboa.pedala.estacoes` | Mantém o estado operacional das estações e das suas docas físicas (ocupação, trincos, docas ativas/inativas), gere a cache local de autorizações e gere o buffer de eventos offline. | Não mantém o histórico de percursos das viagens nem gera perfis fiscais de utilizadores. |
| **`Manutencao`** | `pt.ulisboa.pedala.manutencao` | Monitoriza anomalias reportadas por telemetria ou na devolução, emite ordens de reparação para a equipa de terreno e comanda o bloqueio preventivo de bicicletas na doca. | Não gere o pagamento de viagens nem interfere no fluxo regular de faturação. |
| **`TarifarioCobranca`** | `pt.ulisboa.pedala.faturacao` | Aplica as regras de tarifário (passes, minutos consumidos, penalizações), processa cobranças, gera faturas certificadas e assegura a retenção fiscal de 10 anos para a Autoridade Tributária. | Não recolhe nem armazena as coordenadas geográficas dos trajetos (proteção RGPD) e não decide autorizações de desbloqueio físico. |
| **`RelatoriosMunicipais`** | `pt.ulisboa.pedala.relatorios` | Consolida indicadores agregados de utilização (viagens por freguesia, taxas de rotação de bicicletas, ocupação média) para consumo pela Câmara Municipal e futura API de dados abertos. | Não tem acesso a dados de identificação fiscal (NIF) nem a identificadores individuais de utilizadores. |

---

## 3. Mapeamento das User Stories

As três User Stories selecionadas cobrem fluxos fim-a-fim e cruzam múltiplos componentes lógicos, cumprindo o requisito de envolver 3 ou mais componentes:

| User Story | Componentes Envolvidos | Fluxo de Interação |
| :--- | :--- | :--- |
| **US-1 (Obrigatória): Desbloqueio, Devolução e Faturação** | `GestaoViagens`<br>`EstacoesDocas`<br>`TarifarioCobranca` | 1. O utilizador solicita desbloqueio na app/cartão.<br>2. `GestaoViagens` valida autorização e solicita a `EstacoesDocas` a abertura do trinco.<br>3. `EstacoesDocas` confirma o desbloqueio físico e inicia o acompanhamento da viagem.<br>4. Na devolução, `EstacoesDocas` tranca a bicicleta e emite o evento de fim de viagem.<br>5. `GestaoViagens` calcula a duração e notifica `TarifarioCobranca` para processamento e emissão de fatura. |
| **US-2: Ordem de Manutenção por Alerta de Avaria** | `Manutencao`<br>`EstacoesDocas`<br>`GestaoViagens` | 1. Uma bicicleta elétrica emite telemetria com anomalia de bateria/travões (ou o utilizador reporta avaria ao trancar na doca).<br>2. `GestaoViagens` ou `EstacoesDocas` encaminha o evento de anomalia para `Manutencao`.<br>3. `Manutencao` gera uma ordem de reparação e instrui `EstacoesDocas` a bloquear a doca/bicicleta para novas viagens.<br>4. O técnico de terreno acede à ordem de trabalho, repara a avaria e liberta o equipamento. |
| **US-4: Relatórios Municipais e Isolamento RGPD** | `RelatoriosMunicipais`<br>`GestaoViagens`<br>`EstacoesDocas` | 1. O gestor municipal solicita métricas de procura por freguesia.<br>2. `RelatoriosMunicipais` agrega dados de viagens e localização das estações sem aceder a NIFs ou dados pessoais.<br>3. Os trajetos e identificadores são anonimizados, produzindo apenas agregações estatísticas por zona e hora. |

---

## 4. Métricas de Acoplamento e Instabilidade

Segundo a formulação de Robert C. Martin e Richards & Ford:
* **$Ca$ (Acoplamento Aferente):** Número de componentes externos que dependem deste componente (responsabilidade).
* **$Ce$ (Acoplamento Eferente):** Número de componentes externos dos quais este componente depende (dependência).
* **Instabilidade ($I$):** $I = \frac{Ce}{Ca + Ce}$, variando entre 0 (máxima estabilidade) e 1 (máxima instabilidade).

| Componente | $Ca$ (Incoming) | $Ce$ (Outgoing) | Instabilidade ($I$) | Classificação | Justificação Arquitetural |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **`EstacoesDocas`** | 3 (`GestaoViagens`, `Manutencao`, `RelatoriosMunicipais`) | 0 | **0.00** | Máxima Estabilidade | Representa o estado fundamental dos ativos físicos e das docas e não depende de nenhum outro componente. Quando precisa de avisar os outros (por exemplo, que o encaixe foi confirmado), publica um evento na sua própria API em vez de chamar `GestaoViagens`, e por isso o $Ce$ se mantém em 0. Como três componentes dependem dela, incluindo o caminho crítico do desbloqueio, a sua API é o contrato que mais custa a alterar. |
| **`GestaoViagens`** | 3 (`Manutencao`, `TarifarioCobranca`, `RelatoriosMunicipais`) | 1 (`EstacoesDocas`) | **0.25** | Estável | Orquestra o ciclo da viagem mas só depende de `EstacoesDocas`, para comandar o trinco. `TarifarioCobranca` e `Manutencao` consomem os seus eventos sem que `GestaoViagens` os conheça, e `RelatoriosMunicipais` lê apenas registos anonimizados. A falha ou a alteração de qualquer um desses três não afeta o desbloqueio nem a devolução. O $Ca$ de 3 obriga a manter estáveis os seus eventos e os registos anonimizados. |
| **`Manutencao`** | 0 | 2 (`GestaoViagens`, `EstacoesDocas`) | **1.00** | Máxima Instabilidade | Reage aos alertas de avaria publicados por `GestaoViagens` e instrui `EstacoesDocas` a bloquear a doca ou a bicicleta. Ninguém depende dela, por isso pode evoluir (novos tipos de avaria, novas ordens de trabalho) sem impacto noutros componentes. Uma falha em `Manutencao` não chega ao fluxo de desbloqueio. |
| **`TarifarioCobranca`** | 0 | 1 (`GestaoViagens`) | **1.00** | Máxima Instabilidade | Subscreve `ViagemConcluida` e só depende de `GestaoViagens` através desse evento, que não contém o percurso (RGPD). Ninguém depende dele, por isso as regras de tarifário (blocos de 15 minutos, tarifários de outras câmaras) mudam sem afetar mais ninguém. A indisponibilidade da Autoridade Tributária fica contida aqui e nunca bloqueia a devolução. |
| **`RelatoriosMunicipais`** | 0 | 2 (`GestaoViagens`, `EstacoesDocas`) | **1.00** | Máxima Instabilidade | Componente puramente consumidor e analítico: lê apenas os registos anonimizados de `GestaoViagens` e os dados de consulta de `EstacoesDocas`. Não depende de `TarifarioCobranca`, o que garante que não acede a NIFs. Alterações nesses dois contratos afetam-no, mas ele não afeta ninguém. |

---

## 5. Análise de Conascência

A conascência quantifica o impacto e acoplamento entre os componentes do sistema:

### Conascência Estática
* **Conascência de Nome (CoN):** Presente nas interfaces de serviço Java públicas entre componentes (ex.: `EstacaoService.solicitarDesbloqueio()` e `ManutencaoService.registarAlerta()`). É a forma de conascência mais fraca e recomendada.
* **Conascência de Tipo (CoT):** Presente na partilha de *records* imutáveis de domínio (ex.: `ViagemId`, `DocaId`, `TimestampUTC`), garantindo validação em tempo de compilação sem partilhar estado mutável ou entidades de base de dados.

### Conascência Dinâmica
* **Conascência de Execução / Ordem (CoE):** Presente no ciclo de vida de aluguer: uma bicicleta só pode ser trancada ou encaminhada para manutenção se tiver existido um registo prévio de viagem ativa ou estado de avaria. Mitigada através do uso de identificadores de correlação únicos por viagem.
* **Conascência de Valor (CoV):** A determinação da duração em `GestaoViagens` e a faturação correspondente em `TarifarioCobranca` dependem de carimbos temporais idênticos. Mitigada impondo carimbos temporais normalizados em UTC no instante em que as docas físicas detetam o evento mecânico.

---

## 6. Acoplamento Temporal e Mecanismos de Mitigação

Foram identificados dois pontos críticos de acoplamento temporal no sistema:

1. **Processamento de Faturas no Fecho da Viagem:**
   * *Problema:* Se a comunicação entre `GestaoViagens` e `TarifarioCobranca` for síncrona em bloco transacional, qualquer lentidão de resposta ou indisponibilidade na comunicação com a Autoridade Tributária bloquearia o utilizador na doca ao devolver a bicicleta.
   * *Mitigação:* `GestaoViagens` emite o evento interno `ViagemConcluidaEvent` e encerra a interação física na doca de imediato. O módulo `TarifarioCobranca` subscreve o evento e gera a fatura assincronamente em background.

2. **Propagação de Alertas de Avaria e Bloqueio Preventivo (US-2):**
   * *Problema:* Se o processamento de um alerta de avaria (reportado pela telemetria da bicicleta elétrica) exigir resposta síncrona de confirmação do sistema de manutenção antes de libertar os recursos de rede, a doca e o servidor arriscam timeouts sob pico de tráfego.
   * *Mitigação:* O reporte de anomalia é publicado como evento assíncrono `AlertaAvariaEvent`; `Manutencao` processa a mensagem, abre a ordem de serviço e envia comando autónomo para `EstacoesDocas` colocar a doca/bicicleta em modo bloqueado.

---

## 7. Iteração de Reestruturação Arquitetural

* **Desenho Inicial (Iteração 0):** Existia um único componente de grande dimensão chamado `OperacoesViagem` que acumulava o desbloqueio de docas, a receção de alertas de manutenção e o cálculo direto de faturas.
* **Problemas Detetados:**
  1. *Entity Trap:* Agrupava na mesma estrutura entidades heterogéneas (`Bicicleta`, `Doca`, `Utilizador`, `OrdemTrabalho`), gerando acoplamento excessivo e instabilidade imprevisível.
  2. *Violação de Isolamento Legal (RGPD vs. AT):* Misturava dados fiscais confidenciais com registos de rotas e avarias, impossibilitando a garantia estrita de que os relatórios municipais não acedessem a dados pessoais.
* **Refatoração Aplicada (Iteração 1 - Desenho Atual):**
  * Divisão estrita em componentes desacoplados: `GestaoViagens` (ciclo de aluguer), `EstacoesDocas` (hardware e inventário), `Manutencao` (triagem e ordens) e `TarifarioCobranca` (regras fiscais e tarifárias).
  * **Resultado:** Redução do acoplamento eferente individual, estabilização dos módulos de hardware e conformidade fiscal ($I = 0.00$), e isolamento auditável das regras de privacidade.
