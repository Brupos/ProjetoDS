package pt.ulisboa.pedala.manutencao;

import java.time.Instant;

public record OrdemReparacao(
    String id,
    String bicicletaId,
    String docaId,
    String motivo,
    Instant dataCriacao,
    boolean resolvida
) {}
