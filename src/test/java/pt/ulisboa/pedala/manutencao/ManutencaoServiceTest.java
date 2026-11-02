package pt.ulisboa.pedala.manutencao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.pedala.estacoes.*;

import java.time.Instant;
import java.util.Optional;

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
