package pt.ulisboa.pedala.manutencao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.pedala.estacoes.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ManutencaoServiceTest {

    private DocaService docaService;
    private ManutencaoService manutencaoService;

    @BeforeEach
    void setUp() {
        docaService = new DocaService();
        docaService.registarDoca(new Doca("DOCA-01", "ESTACAO-SALDANHA", EstadoDoca.OCUPADA, "BIKE-100"));
        docaService.registarBicicleta(new Bicicleta("BIKE-100", EstadoBicicleta.DISPONIVEL, 85));
        manutencaoService = new ManutencaoService(docaService);
    }

    @Test
    @DisplayName("US-2: Bloqueio preventivo automático ao registar avaria")
    void deveBloquearDocaEBicicletaAoReportarAvaria() {
        OrdemReparacao ordem = manutencaoService.reportarAvaria("BIKE-100", "DOCA-01", "Travao dianteiro solto");

        assertNotNull(ordem.id());
        assertFalse(ordem.resolvida());
        assertEquals(EstadoDoca.BLOQUEADA_AVARIA, docaService.consultarEstado("DOCA-01"));
        assertEquals(EstadoBicicleta.AVARIADA, docaService.obterBicicleta("BIKE-100").get().getEstado());
        assertEquals(1, manutencaoService.listarOrdensPendentes().size());
    }

    @Test
    @DisplayName("US-2: Processamento via evento desacoplado (ADR-006)")
    void deveProcessarEventoDeAvaria() {
        AvariaReportadaEvent evento = new AvariaReportadaEvent(
            "EVT-01", "BIKE-100", "DOCA-01", "Bloqueio eletromagnético avariado", Instant.now()
        );

        OrdemReparacao ordem = manutencaoService.processarEventoAvaria(evento);

        assertEquals("BIKE-100", ordem.bicicletaId());
        assertEquals(EstadoDoca.BLOQUEADA_AVARIA, docaService.consultarEstado("DOCA-01"));
    }

    @Test
    @DisplayName("US-2: Eventos repetidos são idempotentes mesmo sob concorrência")
    void deveDeduplicarEventosConcorrentes() throws Exception {
        AvariaReportadaEvent evento = new AvariaReportadaEvent(
            "EVT-CONCORRENTE", "BIKE-100", "DOCA-01", "Falha na bateria", Instant.now()
        );
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            ArrayList<Future<OrdemReparacao>> resultados = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                resultados.add(executor.submit(() -> {
                    inicio.await();
                    return manutencaoService.processarEventoAvaria(evento);
                }));
            }
            inicio.countDown();

            String ordemId = resultados.get(0).get().id();
            for (Future<OrdemReparacao> resultado : resultados) {
                assertEquals(ordemId, resultado.get().id());
            }
            assertEquals(1, manutencaoService.listarTodasOrdens().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("US-2: Reportes distintos concorrentes não são perdidos")
    void deveRegistarTodosOsEventosConcorrentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            ArrayList<Future<OrdemReparacao>> resultados = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                int numeroEvento = i;
                resultados.add(executor.submit(() -> {
                    inicio.await();
                    return manutencaoService.processarEventoAvaria(new AvariaReportadaEvent(
                        "EVT-" + numeroEvento,
                        "BIKE-100",
                        "DOCA-01",
                        "Avaria " + numeroEvento,
                        Instant.now()
                    ));
                }));
            }

            assertTimeout(Duration.ofSeconds(3), () -> {
                inicio.countDown();
                for (Future<OrdemReparacao> resultado : resultados) {
                    assertNotNull(resultado.get());
                }
            });
            assertEquals(50, manutencaoService.listarTodasOrdens().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("US-2: Não cria ordens nem bloqueia recursos inexistentes")
    void deveRejeitarEquipamentosInexistentes() {
        assertThrows(IllegalArgumentException.class,
            () -> manutencaoService.reportarAvaria("BIKE-INEXISTENTE", "DOCA-01", "Falha"));
        assertThrows(IllegalArgumentException.class,
            () -> manutencaoService.reportarAvaria("BIKE-100", "DOCA-INEXISTENTE", "Falha"));
        assertTrue(manutencaoService.listarTodasOrdens().isEmpty());
        assertEquals(EstadoDoca.OCUPADA, docaService.consultarEstado("DOCA-01"));
        assertEquals(EstadoBicicleta.DISPONIVEL, docaService.obterBicicleta("BIKE-100").orElseThrow().getEstado());
    }

    @Test
    @DisplayName("US-2: Rejeita identificadores de evento reutilizados com conteúdo diferente")
    void deveRejeitarEventoComIdDuplicadoEConteudoDiferente() {
        AvariaReportadaEvent evento = new AvariaReportadaEvent(
            "EVT-01", "BIKE-100", "DOCA-01", "Falha na bateria", Instant.now()
        );
        manutencaoService.processarEventoAvaria(evento);

        AvariaReportadaEvent eventoConflitante = new AvariaReportadaEvent(
            "EVT-01", "BIKE-100", "DOCA-01", "Falha nos travões", evento.timestamp()
        );
        assertThrows(IllegalArgumentException.class,
            () -> manutencaoService.processarEventoAvaria(eventoConflitante));
        assertEquals(1, manutencaoService.listarTodasOrdens().size());
    }

    @Test
    @DisplayName("US-2: Conclusão de intervenção técnica e libertação de equipamentos")
    void deveConcluirReparacaoEReliberarRecursos() {
        OrdemReparacao ordem = manutencaoService.reportarAvaria("BIKE-100", "DOCA-01", "Pneu furado");

        Optional<OrdemReparacao> concluida = manutencaoService.concluirReparacao(ordem.id(), "Pneu substituído");

        assertTrue(concluida.isPresent());
        assertTrue(concluida.get().resolvida());
        assertEquals("Pneu substituído", concluida.get().relatorioTecnico());
        assertEquals(EstadoDoca.LIVRE, docaService.consultarEstado("DOCA-01"));
        assertEquals(EstadoBicicleta.DISPONIVEL, docaService.obterBicicleta("BIKE-100").get().getEstado());
        assertTrue(manutencaoService.listarOrdensPendentes().isEmpty());
    }
}
