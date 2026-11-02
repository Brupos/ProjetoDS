package pt.ulisboa.pedala.viagens;

import java.time.Instant;

public record ViagemConcluidaEvent(
    String viagemId,
    String ciclistaId,
    String docaOrigemId,
    String docaDestinoId,
    Instant dataInicio,
    Instant dataFim,
    long duracaoMinutos
) {}
