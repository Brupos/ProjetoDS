package pt.ulisboa.pedala.manutencao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.pedala.estacoes.DocaService;
import pt.ulisboa.pedala.estacoes.EstadoDoca;

import static org.junit.jupiter.api.Assertions.*;

class ManutencaoServiceTest {

    private DocaService docaService;
    private ManutencaoService manutencaoService;

    @BeforeEach
    void setUp() {
        docaService = new DocaService();
        docaService.registarDoca("DOCA-01", EstadoDoca.LIVRE);
        manutencaoService = new ManutencaoService(docaService);
    }

    @Test
    @DisplayName("Ao reportar avaria na doca, a mesma deve ficar bloqueada preventivamente")
    void deveBloquearDocaAoReportarAvaria() {
        OrdemReparacao ordem = manutencaoService.reportarAvaria("BIKE-100", "DOCA-01", "Travao danificado");

        assertNotNull(ordem.id());
        assertEquals(EstadoDoca.BLOQUEADA_AVARIA, docaService.consultarEstado("DOCA-01"));
        assertFalse(ordem.resolvida());
        assertEquals(1, manutencaoService.listarOrdensPendentes().size());
    }
}
