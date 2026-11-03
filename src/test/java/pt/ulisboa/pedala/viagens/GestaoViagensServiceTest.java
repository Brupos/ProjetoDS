
package pt.ulisboa.pedala.viagens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.ulisboa.pedala.estacoes.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GestaoViagensServiceTest {

    private DocaService docaService;
    private GestaoViagensService gestao;

    @BeforeEach
    void setUp() {
        docaService = new DocaService();

        gestao = new GestaoViagensService(
                docaService,
                utilizadorId -> true,
                evento -> {}
        );

        docaService.registarBicicleta(
                new Bicicleta("B1", EstadoBicicleta.DISPONIVEL, 100)
        );

        docaService.registarDoca(
                new Doca("D1", "E1", EstadoDoca.OCUPADA, "B1")
        );

        docaService.registarDoca(
                new Doca("D2", "E1", EstadoDoca.LIVRE, null)
        );
    }

    @Test
    void deveIniciarViagemERetirarBicicletaDaDoca() {
        Viagem viagem = gestao.iniciarViagem(
                "U1", "D1", Instant.parse("2026-11-02T10:00:00Z")
        );

        assertNotNull(viagem.id());
        assertEquals("B1", viagem.bicicletaId());
        assertEquals(EstadoBicicleta.EM_VIAGEM,
                docaService.obterBicicleta("B1").orElseThrow().getEstado());
        assertEquals(EstadoDoca.LIVRE, docaService.consultarEstado("D1"));
    }

    @Test
    void deveTerminarViagemEDevolverBicicleta() {
        Viagem viagem = gestao.iniciarViagem(
                "U1", "D1", Instant.parse("2026-11-02T10:00:00Z")
        );

        ViagemConcluidaEvent evento = gestao.terminarViagem(
                viagem.id(), "D2", Instant.parse("2026-11-02T10:45:00Z")
        );

        assertEquals(45, evento.duracaoMinutos());
        assertEquals("D2", evento.docaDestinoId());
        assertEquals(EstadoDoca.OCUPADA, docaService.consultarEstado("D2"));
        assertEquals(EstadoBicicleta.DISPONIVEL,
                docaService.obterBicicleta("B1").orElseThrow().getEstado());
    }

    @Test
    void naoDeveIniciarViagemSemBicicletaDisponivel() {
        assertThrows(IllegalStateException.class,
                () -> gestao.iniciarViagem(
                        "U1", "D2", Instant.parse("2026-11-02T10:00:00Z")
                ));
    }

    @Test
    void naoDeveIniciarViagemComUtilizadorNaoAutorizado() {
        GestaoViagensService gestaoSemAutorizacao =
                new GestaoViagensService(
                        docaService,
                        utilizadorId -> false,
                        evento -> {}
                );

        assertThrows(IllegalStateException.class,
                () -> gestaoSemAutorizacao.iniciarViagem(
                        "U1", "D1", Instant.parse("2026-11-02T10:00:00Z")
                ));
    }
}