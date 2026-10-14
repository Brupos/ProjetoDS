# 01 - Características Arquiteturais (Lisboa Pedala)

## 1. Worksheet de Características Candidatas

| Frase do Enunciado | Característica Explícita | Característica Implícita | Justificação / Impacto Arquitetural |
| :--- | :--- | :--- | :--- |
| "Cerca de 70% dos desbloqueios acontecem entre as 8h00-9h30 e as 17h30-19h00. Em eventos (Santos Populares, maratona) a procura em algumas estações multiplica por 4." | Elasticidade | Escalabilidade | O sistema sofre variações acentuadas de carga concentradas em janelas curtas e em estações específicas, exigindo dimensionamento dinâmico sem estrangular nos picos de procura. |
| "As docas comunicam por rede móvel e há zonas onde a rede falha com frequência. Cada doca guarda até 24 h de eventos localmente." | Tolerância a Falhas | Resiliência / Consistência Eventual | Falhas de rede móvel são parte da rotina operacional; as docas precisam de aceitar operações localmente e reconciliar as viagens mais tarde sem perdas. |
| "O contrato de concessão aplica uma multa por cada hora em que mais de 10% das estações não conseguem desbloquear bicicletas." | Disponibilidade | Confiabilidade | Há penalizações financeiras diretas caso o sistema central ou as estações fiquem indisponíveis, obrigando a redundância e isolamento de falhas. |
| "1 500 bicicletas (70% elétricas) em 180 estações. As elétricas enviam telemetria a cada 30 s em andamento." | Desempenho (Throughput) | Eficiência no Processamento | Tráfego contínuo de leituras de bateria e localização (~1 050 bicicletas elétricas) que não pode competir por recursos nem atrasar os desbloqueios. |
| "As faturas são comunicadas à Autoridade Tributária e guardadas durante 10 anos. O percurso de cada viagem é dado pessoal (RGPD)." | Conformidade Legal (Compliance) | Segurança / Privacidade de Dados | Separação clara entre a faturação (imutável e arquivada por 10 anos) e os trajetos dos utilizadores (dados sensíveis que exigem anonimização). |
| "Dentro de um ano, a Câmara quer publicar a ocupação das estações como dados abertos. Duas câmaras vizinhas mostraram interesse em usar a plataforma com tarifários próprios." | Extensibilidade | Configurabilidade / Interoperabilidade | O desenho dos módulos de tarifário e de partilha de dados deve permitir ligar novas regras de negócio sem alterar o núcleo da aplicação. |
| "Equipa de desenvolvimento de 6 pessoas e orçamento fixo." | Viabilidade (Feasibility) | Simplicidade Arquitetural | Restrição de contexto: impede soluções excessivamente complexas (como microsserviços repartidos) face ao tempo e tamanho reduzido da equipa. |

---

## 2. Top 3 Características Arquiteturais Condutoras

*(As 3 características, apresentadas sem ordenação)*

* **Disponibilidade / Resiliência (Tolerância a falhas de conectividade)**
* **Elasticidade (Absorção de picos de procura)**
* **Segurança e Conformidade Legal (RGPD e Autoridade Tributária)**

---

## 3. Outras Características Consideradas

* **Desempenho / Throughput (Telemetria):** Relevante para processar o estado das baterias em tempo real, mas pode ser tratado em background de forma assíncrona, sem ditar as decisões nucleares de arquitetura.
* **Extensibilidade / Configurabilidade (Outros municípios e Dados Abertos):** É uma evolução planeada para médio prazo ("dentro de um ano"), pelo que deve ser acomodada com bom encapsulamento modular, mas não dita as prioridades imediatas.
* **Simplicidade Arquitetural / Viabilidade:** Funciona como restrição pragmática para manter o sistema sustentável para 6 pessoas.

---

## 4. Decomposição de Característica Composta

* **Confiabilidade Operacional:**
  * **Autonomia Local da Doca:** Capacidade física da doca trancar ou destrancar a bicicleta mesmo sem contacto com o servidor central.
  * **Consistência Eventual:** Capacidade do backend processar e ordenar até 24h de eventos pendentes quando a rede volta, sem criar viagens duplicadas nem erros de cálculo.
  * **Integridade Transacional:** Garantia de que a viagem (início, fim e cobrança) fecha um ciclo de vida coerente no domínio de negócio.

---

## 5. Cenários Concretos para as Top 3

### Cenário 1: Disponibilidade / Resiliência (Operação em Doca sem Rede)
* **Ambiente:** A ligação móvel de uma estação falha durante 3 horas.
* **Estímulo:** Um utilizador passa o cartão Navegante na doca para desbloquear uma bicicleta e, mais tarde, outro utilizador encaixa uma bicicleta numa doca livre da mesma estação.
* **Resposta:** A doca efetua a validação mecânica local, confirma a operação ao utilizador através dos indicadores visuais/sonoros e armazena os eventos na memória local de 24h.
* **Medição:** 0% de devoluções rejeitadas por falta de rede; reconciliação de todas as viagens no servidor em menos de 60 segundos após o regresso da conectividade, sem registos duplicados.

### Cenário 2: Elasticidade (Pico de Procura em Evento Municipal)
* **Ambiente:** Fim de um jogo ou concerto num estádio da cidade às 23h00.
* **Estímulo:** Os pedidos de desbloqueio aumentam 4 vezes em 15 minutos nas estações circundantes.
* **Resposta:** O serviço de desbloqueio processa os pedidos prioritariamente, mantendo a ingestão de telemetria secundária para não saturar as ligações.
* **Medição:** Taxa de sucesso de desbloqueios superior a 99% e tempo de resposta inferior a 2 segundos no percentil 95 ($p95$).

### Cenário 3: Segurança e Conformidade (Isolamento de Dados Fiscais e Trajetos)
* **Ambiente:** Emissão mensal de relatórios municipais e envio de faturas à Autoridade Tributária.
* **Estímulo:** Consulta de relatórios de utilização por freguesia em simultâneo com o fecho contabilístico.
* **Resposta:** Os relatórios são gerados apenas sobre dados agregados e sem identificadores pessoais; os trajetos das viagens são desvinculados dos perfis fiscais e as faturas são seladas e retidas durante 10 anos.
* **Medição:** 0 dados pessoais expostos nos relatórios públicos; 100% das faturas emitidas e comunicadas à AT dentro dos prazos legais.
