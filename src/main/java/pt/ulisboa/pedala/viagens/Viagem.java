package pt.ulisboa.pedala.viagens;

import java.time.Instant;

public record Viagem(
    String id,
    String utilizadorId,
    String bicicletaId,
    String docaOrigemId,
    Instant inicio,
    Instant fim
) {}
