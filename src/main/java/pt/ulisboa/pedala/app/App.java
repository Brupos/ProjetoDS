
package pt.ulisboa.pedala.app;

import pt.ulisboa.pedala.estacoes.*;
import pt.ulisboa.pedala.viagens.*;
import pt.ulisboa.pedala.faturacao.FaturaService;

import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        DocaService docaService = new DocaService();
        FaturaService faturaService = new FaturaService();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        PublicadorViagemConcluida publicador = evento ->
            executor.execute(() -> {
                var tarifa = faturaService.calcularTarifa(evento);
                System.out.println("\n[Faturação] Viagem "
                        + evento.viagemId()
                        + " - Tarifa: " + tarifa + " €");
            });

        AutorizadorViagem autorizador = utilizadorId -> true;

        GestaoViagensService gestao = new GestaoViagensService(
                docaService,
                autorizador,
                publicador
        );

        // Dados iniciais para demonstração
        docaService.registarEstacao(new Estacao("E1", "Alvalade"));

        docaService.registarBicicleta(
                new Bicicleta("B1", EstadoBicicleta.DISPONIVEL, 100)
        );

        docaService.registarDoca(
                new Doca("D1", "E1", EstadoDoca.OCUPADA, "B1")
        );

        docaService.registarDoca(
                new Doca("D2", "E1", EstadoDoca.LIVRE, null)
        );

        String viagemAtiva = null;

        System.out.println("=================================");
        System.out.println("         LISBOA PEDALA");
        System.out.println("=================================");

        try {
            boolean executar = true;

            while (executar) {
                System.out.println("\n1. Iniciar viagem");
                System.out.println("2. Terminar viagem");
                System.out.println("3. Consultar estado das docas");
                System.out.println("0. Sair");
                System.out.print("\nOpção: ");

                String opcao = scanner.nextLine();

                switch (opcao) {
                    case "1" -> {
                        System.out.print("ID do utilizador: ");
                        String utilizadorId = scanner.nextLine();

                        System.out.print("ID da doca de origem: ");
                        String docaId = scanner.nextLine();

                        try {
                            Viagem viagem = gestao.iniciarViagem(
                                    utilizadorId,
                                    docaId,
                                    Instant.now()
                            );

                            viagemAtiva = viagem.id();

                            System.out.println("\nViagem iniciada!");
                            System.out.println("ID: " + viagem.id());
                            System.out.println("Bicicleta: "
                                    + viagem.bicicletaId());

                        } catch (RuntimeException e) {
                            System.out.println("Erro: " + e.getMessage());
                        }
                    }

                    case "2" -> {
                        if (viagemAtiva == null) {
                            System.out.println("Não existe uma viagem ativa.");
                            break;
                        }

                        System.out.print("ID da doca de destino: ");
                        String docaDestino = scanner.nextLine();

                        try {
                            ViagemConcluidaEvent evento =
                                    gestao.terminarViagem(
                                            viagemAtiva,
                                            docaDestino,
                                            Instant.now()
                                    );

                            System.out.println("\nViagem terminada!");
                            System.out.println("Duração: "
                                    + evento.duracaoMinutos() + " minutos");

                            viagemAtiva = null;

                        } catch (RuntimeException e) {
                            System.out.println("Erro: " + e.getMessage());
                        }
                    }

                    case "3" -> {
                        System.out.println("\nEstado das docas:");
                        System.out.println("D1: "
                                + docaService.consultarEstado("D1"));
                        System.out.println("D2: "
                                + docaService.consultarEstado("D2"));
                    }

                    case "0" -> executar = false;

                    default -> System.out.println("Opção inválida.");
                }
            }
        } finally {
            executor.shutdown();
            scanner.close();
        }

        System.out.println("Aplicação encerrada.");
    }
}