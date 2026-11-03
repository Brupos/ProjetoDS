# Registo de Violações de Código Gerado por IA

## Violação 1 — Tempo gratuito incorreto na faturação

- **Componente:** `FaturaService.java` (`faturacao`)
- **Objetivo:** Implementar o cálculo da tarifa de uma viagem concluída, respeitando as regras de faturação dos assinantes.
- **Pedido feito à IA:** Implementar o cálculo da tarifa de uma viagem concluída, considerando o período gratuito e a cobrança por blocos adicionais de 15 minutos, de acordo com os requisitos e a arquitetura do projeto.
- **Ficheiros de contexto fornecidos:** `ARCHITECTURE.md` e ficheiros relevantes dos módulos `viagens` e `faturacao`, incluindo a especificação das regras de faturação.
- **Resultado gerado:** A implementação considerava 30 minutos gratuitos para assinantes, em vez dos 45 minutos definidos na especificação.
- **Avaliação:** Resultado rejeitado inicialmente e corrigido, por não respeitar o requisito funcional relativo ao período gratuito.
- **Violação da especificação:** O código gerado não respeitou o período de gratuitidade definido para assinantes.
- **Deteção:** Revisão humana.
- **Correção:** Alterar o limite de gratuitidade de 30 para 45 minutos.

## Violação 2 — Dependência circular entre manutenção e viagens

- **Componente:** `ManutencaoService.java` (`manutencao`)
- **Objetivo:** Implementar o processamento de reportes de avarias, a criação de ordens de reparação e o bloqueio preventivo de bicicletas.
- **Pedido feito à IA:** Implementar o serviço de manutenção para processar avarias e gerir as respetivas ordens de reparação, respeitando a arquitetura modular do projeto.
- **Ficheiros de contexto fornecidos:** `ARCHITECTURE.md`, ADR-002, ADR-003, ADR-006 e ficheiros relevantes dos módulos `manutencao`, `viagens` e `estacoes`.
- **Resultado gerado:** A IA tentou injetar diretamente `ViagensService` em `ManutencaoService`, para terminar uma viagem em curso quando era reportada uma avaria.
- **Avaliação:** Resultado rejeitado inicialmente e reformulado, por violar as regras de dependência entre módulos.
- **Violação da especificação:** A solução introduzia uma dependência síncrona desnecessária e contrariava o ADR-006, que impede o módulo `viagens` de depender de `manutencao`, evitando dependências circulares.
- **Deteção:** Revisão humana.
- **Correção:** Fazer com que `ManutencaoService` reaja a um `AvariaReportadaEvent` e atue sobre o módulo `estacoes`, sem controlar diretamente o ciclo de vida das viagens.

## Violação 3 — Dependências externas no módulo Estações

- **Componente:** `Bicicleta.java` / `Doca.java` (`estacoes`)
- **Objetivo:** Representar bicicletas, docas e respetivos estados físicos, incluindo as operações de bloqueio e desbloqueio.
- **Pedido feito à IA:** Implementar as classes do módulo `estacoes`, garantindo o isolamento do módulo e respeitando as regras de dependência da arquitetura.
- **Ficheiros de contexto fornecidos:** `ARCHITECTURE.md`, ADR-002, ADR-004, ADR-006 e estrutura dos módulos do projeto.
- **Resultado gerado:** A IA introduziu dependências de classes externas relacionadas com utilizadores ou viagens, para guardar na doca a identificação do último utilizador.
- **Avaliação:** Resultado rejeitado inicialmente e corrigido, por violar o isolamento do módulo.
- **Violação da especificação:** O módulo `estacoes` deveria manter `Ce = 0` e `I = 0.00`, sem dependências de outros módulos de negócio.
- **Deteção:** Revisão humana.
- **Correção:** Restringir as classes `Bicicleta` e `Doca` à representação dos estados físicos e à utilização de identificadores simples, sem importar entidades ou serviços de outros módulos.

## Violação 4 — Fitness function demasiado permissiva

- **Componente:** `ArchitectureTest` (`tests/architecture`)
- **Objetivo:** Criar testes automáticos com ArchUnit para verificar as regras `allowed-to-use` e a ausência de ciclos de dependência entre módulos.
- **Pedido feito à IA:** Implementar testes de arquitetura com ArchUnit para validar as regras de dependência definidas no ADR-006, incluindo o isolamento do módulo `estacoes`.
- **Ficheiros de contexto fornecidos:** `ARCHITECTURE.md`, ADR-002, ADR-006 e estrutura de pacotes do projeto.
- **Resultado gerado:** A regra de isolamento de `estacoes` era demasiado permissiva e não detetava corretamente determinadas dependências externas, permitindo falsos positivos.
- **Avaliação:** Resultado rejeitado inicialmente e reformulado, por não garantir a validação efetiva das regras arquiteturais.
- **Violação da especificação:** A fitness function não impedia de forma fiável dependências proibidas, comprometendo a verificação automática da arquitetura.
- **Deteção:** Revisão humana, através da análise da regra e do seu comportamento.
- **Correção:** Reforçar a regra com `onlyDependOnClassesThat()`, restringindo as dependências às classes permitidas do módulo `estacoes` e às bibliotecas base de Java (`java..`).

---

## Observação

As violações foram identificadas durante a revisão do código e das regras de arquitetura geradas por IA. Os casos 1, 2 e 3 correspondem a violações do código dos componentes; o caso 4 corresponde a uma falha na própria fitness function gerada.

A documentação das correções deve ser acompanhada pela confirmação de que as alterações foram efetivamente implementadas e validadas através dos testes correspondentes.
