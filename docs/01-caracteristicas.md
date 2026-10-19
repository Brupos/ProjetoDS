# 01 - Características Arquiteturais (Lisboa Pedala)

## 1. Worksheet de Características Candidatas

| Frase do Enunciado | Característica Explícita | Característica Implícita | Justificação / Impacto Arquitetural |
| :--- | :--- | :--- | :--- |
| "Cerca de 70% dos desbloqueios acontecem entre as 8h00-9h30 e as 17h30-19h00. Em eventos (Santos Populares, maratona) a procura em algumas estações multiplica por 4." | Elasticidade | Escalabilidade | O sistema sofre variações acentuadas de carga concentradas em janelas curtas e em estações específicas, exigindo dimensionamento dinâmico sem estrangular nos picos de procura. |
| "As docas comunicam por rede móvel e há zonas onde a rede falha com frequência. Cada doca guarda até 24 h de eventos localmente." | Tolerância a Falhas | Resiliência / Consistência Eventual | Falhas de conectividade ou telemetria são parte da rotina operacional; o sistema precisa de absorver eventos de forma assíncrona e manter a operação das estações sem bloqueios. |
| "O contrato de concessão aplica uma multa por cada hora em que mais de 10% das estações não conseguem desbloquear bicicletas." | Disponibilidade | Confiabilidade | Há penalizações financeiras diretas caso o sistema central ou as estações fiquem indisponíveis, obrigando a redundância e isolamento estrito de falhas de hardware e software. |
| "1 500 bicicletas (70% elétricas) em 180 estações. As elétricas enviam telemetria a cada 30 s em andamento." | Desempenho (Throughput) | Eficiência no Processamento | Tráfego contínuo de leituras de bateria e localização (~1 050 bicicletas elétricas) que não pode competir por recursos nem atrasar os desbloqueios regulares. |
| "As faturas são comunicadas à Autoridade Tributária e guardadas durante 10 anos. O percurso de cada viagem é dado pessoal (RGPD)." | Conformidade Legal (Compliance) | Segurança / Privacidade de Dados | Separação clara entre a faturação (imutável e arquivada por 10 anos) e os trajetos dos utilizadores (dados sensíveis que exigem anonimização para relatórios públicos). |
| "Dentro de um ano, a Câmara quer publicar a ocupação das estações como dados abertos. Duas câmaras vizinhas mostraram interesse em usar a plataforma com tarifários próprios." | Extensibilidade | Configurabilidade / Interoperabilidade | O desenho dos módulos de tarifário e de partilha de dados deve permitir ligar novas regras de negócio sem alterar o núcleo da aplicação. |
| "Equipa de desenvolvimento de 6 pessoas e orçamento fixo." | Viabilidade (Feasibility) | Simplicidade Arquitetural | Restrição de contexto: impede soluções excessivamente complexas (como microsserviços repartidos) face ao tempo e tamanho reduzido da equipa. |

---

## 2. Top 3 Características Arquiteturais Condutoras

* **Disponibilidade / Resiliência (Tolerância a falhas operacionais e manutenção preventiva)**
* **Elasticidade (Absorção de picos de procura e desbloqueios)**
* **Segurança e Conformidade Legal (Isolamento RGPD e Autoridade Tributária)**

---

## 3. Outras Características Consideradas

* **Desempenho / Throughput (Telemetria):** Relevante para processar o estado das baterias em tempo real, mas tratado em background de forma assíncrona, sem ditar as decisões estruturais nucleares.
* **Extensibilidade / Configurabilidade (Outros municípios e Dados Abertos):** É uma evolução planeada a médio prazo, pelo que é acomodada com bom encapsulamento modular no monólito, sem ditar as prioridades imediatas.
* **Simplicidade Arquitetural / Viabilidade:** Funciona como restrição pragmática para manter o sistema sustentável e exequível dentro do calendário e dimensão da equipa.

---

## 4. Decomposição de Característica Composta

* **Confiabilidade Operacional:**
  * **Segregação de Falhas na Doca:** Capacidade de isolar uma doca ou bicicleta avariada sem interromper o serviço regular das restantes posições da estação.
  * **Consistência de Eventos de Manutenção:** Garantia de que alertas emitidos por telemetria ou devolução geram ordens de serviço fiáveis sem bloqueios no fluxo operacional da viagem.
  * **Integridade Transacional:** Garantia de que a viagem (início, fim e cobrança) fecha um ciclo de vida coerente no domínio de negócio.

---

## 5. Cenários Concretos para as Top 3

### Cenário 1: Disponibilidade / Resiliência (Tolerância a Falhas na Triagem de Avarias e Manutenção)
* **Ambiente:** Picos de tráfego em que múltiplas bicicletas elétricas reportam alertas de anomalia mecânica/elétrica em simultâneo (US-2).
* **Estímulo:** Um utilizador reporta avaria ao trancar a bicicleta na doca ou a telemetria a bordo deteta falha crítica de bateria/travões.
* **Resposta:** O sistema regista a anomalia sem bloquear a operação da estação, isola a bicicleta com comando de bloqueio preventivo e emite a respetiva ordem de manutenção para a equipa técnica.
* **Medição:** 100% dos reportes de avaria processados sem perda de eventos e bloqueio preventivo acionado na doca em menos de 3 segundos, mantendo as restantes docas da estação totalmente operacionais para novos alugueres.

### Cenário 2: Elasticidade (Pico de Procura em Evento Municipal)
* **Ambiente:** Fim de um jogo ou concerto num estádio da cidade às 23h00 (US-1).
* **Estímulo:** Os pedidos de desbloqueio aumentam 4 vezes em 15 minutos nas estações circundantes.
* **Resposta:** O serviço de desbloqueio processa os pedidos prioritariamente, mantendo a ingestão de telemetria secundária para não saturar as ligações.
* **Medição:** Taxa de sucesso de desbloqueios superior a 99% e tempo de resposta inferior a 2 segundos no percentil 95 ($p95$).

### Cenário 3: Segurança e Conformidade (Isolamento de Dados Fiscais e Trajetos RGPD)
* **Ambiente:** Emissão mensal de relatórios municipais por freguesia em simultâneo com o fecho contabilístico da faturação (US-4).
* **Estímulo:** Consulta de relatórios de utilização por freguesia em simultâneo com a emissão e envio de faturas à Autoridade Tributária.
* **Resposta:** Os relatórios municipais são gerados exclusivamente sobre dados agregados sem identificadores pessoais; os trajetos das viagens são desvinculados dos perfis fiscais e as faturas são seladas e retidas durante 10 anos.
* **Medição:** 0 dados pessoais expostos nos relatórios públicos; 100% das faturas emitidas e comunicadas à AT dentro dos prazos legais.
