package pt.ulisboa.pedala.manutencao;

import pt.ulisboa.pedala.estacoes.DocaService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ManutencaoService {
    private final DocaService docaService;
    private final List<OrdemReparacao> ordens = new ArrayList<>();

    public ManutencaoService(DocaService docaService) {
        this.docaService = docaService;
    }

    public OrdemReparacao processarEventoAvaria(AvariaReportadaEvent evento) {
        return reportarAvaria(evento.bicicletaId(), evento.docaId(), evento.descricao());
    }

    public OrdemReparacao reportarAvaria(String bicicletaId, String docaId, String motivo) {
        if (docaId != null && !docaId.isBlank()) {
            docaService.bloquearDoca(docaId);
        }
        if (bicicletaId != null && !bicicletaId.isBlank()) {
            docaService.bloquearBicicleta(bicicletaId);
        }

        OrdemReparacao ordem = new OrdemReparacao(
            UUID.randomUUID().toString(),
            bicicletaId,
            docaId,
            motivo,
            Instant.now(),
            null,
            null,
            false
        );
        ordens.add(ordem);
        return ordem;
    }

    public Optional<OrdemReparacao> concluirReparacao(String ordemId, String relatorioTecnico) {
        for (int i = 0; i < ordens.size(); i++) {
            OrdemReparacao atual = ordens.get(i);
            if (atual.id().equals(ordemId)) {
                OrdemReparacao resolvida = atual.concluir(relatorioTecnico);
                ordens.set(i, resolvida);

                if (resolvida.docaId() != null) {
                    docaService.libertarDocaAposReparacao(resolvida.docaId());
                }
                if (resolvida.bicicletaId() != null) {
                    docaService.obterBicicleta(resolvida.bicicletaId())
                               .ifPresent(b -> b.disponibilizar());
                }
                return Optional.of(resolvida);
            }
        }
        return Optional.empty();
    }

    public List<OrdemReparacao> listarOrdensPendentes() {
        return ordens.stream().filter(o -> !o.resolvida()).toList();
    }

    public List<OrdemReparacao> listarTodasOrdens() {
        return List.copyOf(ordens);
    }
}
