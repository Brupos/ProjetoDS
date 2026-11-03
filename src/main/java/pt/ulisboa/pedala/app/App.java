package pt.ulisboa.pedala.app;

import pt.ulisboa.pedala.estacoes.Bicicleta;
import pt.ulisboa.pedala.estacoes.Doca;
import pt.ulisboa.pedala.estacoes.DocaService;
import pt.ulisboa.pedala.estacoes.Estacao;
import pt.ulisboa.pedala.estacoes.EstadoBicicleta;
import pt.ulisboa.pedala.estacoes.EstadoDoca;
import pt.ulisboa.pedala.faturacao.FaturaService;
import pt.ulisboa.pedala.manutencao.AvariaReportadaEvent;
import pt.ulisboa.pedala.manutencao.ManutencaoService;
import pt.ulisboa.pedala.manutencao.OrdemReparacao;
import pt.ulisboa.pedala.relatorios.RelatorioFreguesiaHora;
import pt.ulisboa.pedala.relatorios.RelatorioUtilizacaoService;
import pt.ulisboa.pedala.viagens.AutorizadorViagem;
import pt.ulisboa.pedala.viagens.GestaoViagensService;
import pt.ulisboa.pedala.viagens.PublicadorViagemConcluida;
import pt.ulisboa.pedala.viagens.Viagem;
import pt.ulisboa.pedala.viagens.ViagemConcluidaEvent;
import pt.ulisboa.pedala.viagens.ViagensQueryService;

import java.io.PrintStream;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class App {
    private App() {}

    public static void main(String[] args) {
        run(new Scanner(System.in), System.out);
    }

    static void run(Scanner scanner, PrintStream out) {
        DocaService docaService = new DocaService();
        docaService.registarEstacao(new Estacao("E1", "Alvalade"));
        docaService.registarEstacao(new Estacao("E2", "Areeiro"));
        docaService.registarBicicleta(
            new Bicicleta("B1", EstadoBicicleta.DISPONIVEL, 100)
        );
        docaService.registarDoca(
            new Doca("D1", "E1", EstadoDoca.OCUPADA, "B1")
        );
        docaService.registarDoca(
            new Doca("D2", "E2", EstadoDoca.LIVRE, null)
        );

        ViagensQueryService viagensQueryService = new ViagensQueryService(docaService);
        RelatorioUtilizacaoService relatorioService =
            new RelatorioUtilizacaoService(docaService, viagensQueryService);
        ManutencaoService manutencaoService = new ManutencaoService(docaService);
        FaturaService faturaService = new FaturaService();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Future<?>> publicacoesPendentes = new ArrayList<>();
        AtomicReference<GestaoViagensService> gestaoRef = new AtomicReference<>();

        PublicadorViagemConcluida publicador = evento ->
            publicacoesPendentes.add(executor.submit(() -> {
                GestaoViagensService gestao = gestaoRef.get();
                Viagem viagem = gestao.obterViagem(evento.viagemId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Viagem concluída não encontrada: " + evento.viagemId()
                    ));
                viagensQueryService.registarViagemConcluida(viagem);

                out.println("\n[Faturação] Viagem " + evento.viagemId()
                    + " - Tarifa: " + faturaService.calcularTarifa(evento) + " €");
            }));

        AutorizadorViagem autorizador = utilizadorId -> true;
        GestaoViagensService gestao = new GestaoViagensService(
            docaService,
            autorizador,
            publicador
        );
        gestaoRef.set(gestao);

        String viagemAtiva = null;
        out.println("=================================");
        out.println("         LISBOA PEDALA");
        out.println("=================================");

        try {
            boolean executar = true;
            while (executar) {
                mostrarMenu(out);
                out.flush();
                if (!scanner.hasNextLine()) {
                    break;
                }
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1" -> {
                        if (viagemAtiva != null) {
                            out.println("Já existe uma viagem ativa: " + viagemAtiva);
                            break;
                        }
                        try {
                            String utilizadorId = lerEntrada(scanner, out, "ID do utilizador: ");
                            String docaId = lerEntrada(scanner, out, "ID da doca de origem: ");
                            Viagem viagem = gestao.iniciarViagem(
                                utilizadorId,
                                docaId,
                                Instant.now()
                            );
                            viagemAtiva = viagem.id();
                            out.println("\nViagem iniciada!");
                            out.println("ID: " + viagem.id());
                            out.println("Bicicleta: " + viagem.bicicletaId());
                        } catch (RuntimeException e) {
                            out.println("Erro: " + e.getMessage());
                        }
                    }
                    case "2" -> {
                        if (viagemAtiva == null) {
                            out.println("Não existe uma viagem ativa.");
                            break;
                        }
                        try {
                            String docaDestino = lerEntrada(scanner, out, "ID da doca de destino: ");
                            ViagemConcluidaEvent evento = gestao.terminarViagem(
                                viagemAtiva,
                                docaDestino,
                                Instant.now()
                            );
                            out.println("\nViagem terminada!");
                            out.println("Duração: " + evento.duracaoMinutos() + " minutos");
                            viagemAtiva = null;
                        } catch (RuntimeException e) {
                            out.println("Erro: " + e.getMessage());
                        }
                    }
                    case "3" -> mostrarEstadoDocas(docaService, out);
                    case "4" -> reportarAvaria(scanner, manutencaoService, out);
                    case "5" -> concluirReparacao(scanner, manutencaoService, out);
                    case "6" -> gerarRelatorio(scanner, relatorioService, publicacoesPendentes, out);
                    case "0" -> executar = false;
                    default -> out.println("Opção inválida.");
                }
            }
        } finally {
            try {
                aguardarPublicacoes(publicacoesPendentes);
            } finally {
                executor.shutdown();
            }
        }

        out.println("Aplicação encerrada.");
    }

    private static void mostrarMenu(PrintStream out) {
        out.println("\n1. Iniciar viagem");
        out.println("2. Terminar viagem");
        out.println("3. Consultar estado das docas");
        out.println("4. Reportar avaria");
        out.println("5. Concluir primeira ordem pendente");
        out.println("6. Relatório mensal por freguesia");
        out.println("0. Sair");
        out.print("\nOpção: ");
        out.flush();
    }

    private static void mostrarEstadoDocas(DocaService docaService, PrintStream out) {
        out.println("\nEstado das docas:");
        out.println("D1: " + docaService.consultarEstado("D1"));
        out.println("D2: " + docaService.consultarEstado("D2"));
    }

    private static void reportarAvaria(
        Scanner scanner,
        ManutencaoService manutencaoService,
        PrintStream out
    ) {
        try {
            String bicicletaId = opcional(lerEntrada(scanner, out, "ID da bicicleta (ou -): "));
            String docaId = opcional(lerEntrada(scanner, out, "ID da doca (ou -): "));
            String descricao = lerEntrada(scanner, out, "Descrição da avaria: ");

            OrdemReparacao ordem = manutencaoService.processarEventoAvaria(
                new AvariaReportadaEvent(
                    UUID.randomUUID().toString(),
                    bicicletaId,
                    docaId,
                    descricao,
                    Instant.now()
                )
            );
            out.println("Avaria registada. Ordem: " + ordem.id());
            out.println("Doca: " + (docaId == null ? "não indicada" : docaId + " bloqueada"));
            out.println("Bicicleta: " + (bicicletaId == null ? "não indicada" : "bloqueada"));
        } catch (RuntimeException e) {
            out.println("Erro: " + e.getMessage());
        }
    }

    private static void concluirReparacao(
        Scanner scanner,
        ManutencaoService manutencaoService,
        PrintStream out
    ) {
        List<OrdemReparacao> pendentes = manutencaoService.listarOrdensPendentes();
        if (pendentes.isEmpty()) {
            out.println("Não existem ordens de reparação pendentes.");
            return;
        }

        OrdemReparacao ordem = pendentes.get(0);
        out.println("Ordem selecionada: " + ordem.id());
        try {
            Optional<OrdemReparacao> concluida =
                manutencaoService.concluirReparacao(
                    ordem.id(),
                    lerEntrada(scanner, out, "Relatório técnico: ")
                );
            if (concluida.isPresent()) {
                out.println("Reparação concluída; equipamento libertado.");
            } else {
                out.println("A ordem já não existe.");
            }
        } catch (RuntimeException e) {
            out.println("Erro: " + e.getMessage());
        }
    }

    private static void gerarRelatorio(
        Scanner scanner,
        RelatorioUtilizacaoService relatorioService,
        List<Future<?>> publicacoesPendentes,
        PrintStream out
    ) {
        try {
            aguardarPublicacoes(publicacoesPendentes);
            YearMonth mes = YearMonth.parse(lerEntrada(scanner, out, "Mês do relatório (AAAA-MM): "));
            List<RelatorioFreguesiaHora> relatorio =
                relatorioService.gerarRelatorioMensal(mes);
            if (relatorio.isEmpty()) {
                out.println("Sem viagens para o período.");
                return;
            }

            out.println("\nViagens por freguesia e hora UTC:");
            for (RelatorioFreguesiaHora linha : relatorio) {
                out.printf("%s | %s | %d partidas%n",
                    linha.freguesia(), linha.horaUtc(), linha.totalPartidas());
            }
        } catch (DateTimeParseException e) {
            out.println("Mês inválido. Use o formato AAAA-MM.");
        } catch (RuntimeException e) {
            out.println("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private static String opcional(String valor) {
        String texto = valor.trim();
        return texto.isEmpty() || texto.equals("-") ? null : texto;
    }

    private static String lerEntrada(Scanner scanner, PrintStream out, String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.nextLine().trim();
    }

    private static void aguardarPublicacoes(List<Future<?>> publicacoes) {
        for (Future<?> publicacao : List.copyOf(publicacoes)) {
            try {
                publicacao.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrompido ao aguardar processamento da viagem.", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Falha ao processar viagem concluída.", e.getCause());
            }
        }
        publicacoes.clear();
    }
}
