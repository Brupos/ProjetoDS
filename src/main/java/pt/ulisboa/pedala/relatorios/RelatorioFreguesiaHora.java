package pt.ulisboa.pedala.relatorios;

import java.time.Instant;

public record RelatorioFreguesiaHora(
    String freguesia,
    Instant horaUtc,
    long totalPartidas
) {}
