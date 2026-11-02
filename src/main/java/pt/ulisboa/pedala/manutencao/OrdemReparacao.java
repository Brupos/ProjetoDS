package pt.ulisboa.pedala.manutencao;

import java.time.Instant;

public record OrdemReparacao(
    String id,
    String bicicletaId,
    String docaId,
    String motivo,
    Instant dataCriacao,
    Instant dataConclusao,
    String relatorioTecnico,
    boolean resolvida
) {
    public OrdemReparacao concluir(String relatorio) {
        return new OrdemReparacao(
            id, bicicletaId, docaId, motivo, dataCriacao, Instant.now(), relatorio, true
        );
    }
}
