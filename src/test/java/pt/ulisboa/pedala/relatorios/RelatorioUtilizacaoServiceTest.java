package pt.ulisboa.pedala.relatorios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.pedala.estacoes.Doca;
import pt.ulisboa.pedala.estacoes.DocaService;
import pt.ulisboa.pedala.estacoes.Estacao;
import pt.ulisboa.pedala.estacoes.EstadoDoca;
import pt.ulisboa.pedala.viagens.Viagem;
import pt.ulisboa.pedala.viagens.ViagensQueryService;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioUtilizacaoServiceTest {
    private DocaService docaService;
    private ViagensQueryService viagensQueryService;
    private RelatorioUtilizacaoService relatorioService;

    @BeforeEach
    void setUp() {
        docaService = new DocaService();
        docaService.registarEstacao(new Estacao("ESTACAO-01", "Alvalade"));
        docaService.registarEstacao(new Estacao("ESTACAO-02", "Areeiro"));
        docaService.registarDoca(new Doca("DOCA-01", "ESTACAO-01", EstadoDoca.LIVRE, null));
        docaService.registarDoca(new Doca("DOCA-02", "ESTACAO-01", EstadoDoca.LIVRE, null));
        docaService.registarDoca(new Doca("DOCA-03", "ESTACAO-02", EstadoDoca.LIVRE, null));
        viagensQueryService = new ViagensQueryService(docaService);
        relatorioService = new RelatorioUtilizacaoService(docaService, viagensQueryService);
    }

    @Test
    @DisplayName("US-4: Agrega viagens por freguesia e hora sem expor identificadores")
    void deveGerarIndicadoresAnonimizadosPorFreguesiaEHora() {
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-1", "USER-1", "DOCA-01", "2026-10-12T08:01:00Z")
        );
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-2", "USER-2", "DOCA-02", "2026-10-12T08:59:00Z")
        );
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-3", "USER-3", "DOCA-03", "2026-10-12T08:20:00Z")
        );

        List<RelatorioFreguesiaHora> relatorio =
            relatorioService.gerarRelatorioMensal(YearMonth.of(2026, 10));

        assertEquals(2, relatorio.size());
        assertEquals(new RelatorioFreguesiaHora(
            "Alvalade", Instant.parse("2026-10-12T08:00:00Z"), 2
        ), relatorio.get(0));
        assertEquals(new RelatorioFreguesiaHora(
            "Areeiro", Instant.parse("2026-10-12T08:00:00Z"), 1
        ), relatorio.get(1));
        String conteudo = relatorio.toString();
        assertFalse(conteudo.contains("TRIP-"));
        assertFalse(conteudo.contains("USER-"));
        assertFalse(conteudo.contains("DOCA-"));
    }

    @Test
    @DisplayName("US-4: Exclui viagens fora do mês solicitado")
    void deveRestringirRelatorioAoMesSolicitado() {
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-1", "USER-1", "DOCA-01", "2026-10-31T23:59:00Z")
        );
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-2", "USER-2", "DOCA-01", "2026-11-01T00:00:00Z")
        );

        List<RelatorioFreguesiaHora> relatorio =
            relatorioService.gerarRelatorioMensal(YearMonth.of(2026, 10));

        assertEquals(1, relatorio.size());
        assertEquals(1, relatorio.get(0).totalPartidas());
    }

    @Test
    @DisplayName("US-4: Rejeita viagens sem localização de estação conhecida")
    void deveFalharQuandoAEstacaoNaoTemFreguesiaRegistada() {
        docaService.registarDoca(new Doca("DOCA-SEM-ESTACAO", "ESTACAO-DESCONHECIDA", EstadoDoca.LIVRE, null));
        viagensQueryService.registarViagemConcluida(
            viagem("TRIP-1", "USER-1", "DOCA-SEM-ESTACAO", "2026-10-12T08:01:00Z")
        );

        assertThrows(IllegalStateException.class,
            () -> relatorioService.gerarRelatorioMensal(YearMonth.of(2026, 10)));
    }

    @Test
    @DisplayName("US-4: Ignora reentrega idêntica e rejeita identificador de viagem conflituoso")
    void deveSerIdempotenteAoRegistarViagensConcluidas() {
        Viagem viagem = viagem("TRIP-1", "USER-1", "DOCA-01", "2026-10-12T08:01:00Z");
        viagensQueryService.registarViagemConcluida(viagem);
        viagensQueryService.registarViagemConcluida(viagem);

        assertEquals(1, relatorioService.gerarRelatorioMensal(YearMonth.of(2026, 10))
            .get(0).totalPartidas());

        Viagem conflituosa = viagem("TRIP-1", "USER-2", "DOCA-01", "2026-10-12T08:01:00Z");
        assertThrows(IllegalArgumentException.class,
            () -> viagensQueryService.registarViagemConcluida(conflituosa));
    }

    private static Viagem viagem(String id, String utilizadorId, String docaOrigemId, String inicio) {
        Instant instanteInicio = Instant.parse(inicio);
        return new Viagem(id, utilizadorId, "BIKE-1", docaOrigemId,
            instanteInicio, instanteInicio.plusSeconds(600));
    }
}
