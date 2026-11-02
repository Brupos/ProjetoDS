package pt.ulisboa.pedala.viagens;

import java.time.Instant;

public record ViagemAgregadaPorFreguesiaHora(
    String freguesia,
    Instant horaUtc,
    long totalPartidas
) {}
