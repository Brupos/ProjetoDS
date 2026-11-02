package pt.ulisboa.pedala.manutencao;

import java.time.Instant;

public record AvariaReportadaEvent(
    String eventoId,
    String bicicletaId,
    String docaId,
    String descricao,
    Instant timestamp
) {}
