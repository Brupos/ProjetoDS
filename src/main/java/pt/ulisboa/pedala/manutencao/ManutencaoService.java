package pt.ulisboa.pedala.manutencao;

import pt.ulisboa.pedala.estacoes.DocaService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManutencaoService {
    private final DocaService docaService;
    private final List<OrdemReparacao> ordens = new ArrayList<>();

    public ManutencaoService(DocaService docaService) {
        this.docaService = docaService;
    }

    public OrdemReparacao reportarAvaria(String bicicletaId, String docaId, String motivo) {
        docaService.bloquearDoca(docaId);

        OrdemReparacao ordem = new OrdemReparacao(
            UUID.randomUUID().toString(),
            bicicletaId,
            docaId,
            motivo,
            Instant.now(),
            false
        );
        ordens.add(ordem);
        return ordem;
    }

    public List<OrdemReparacao> listarOrdensPendentes() {
        return ordens.stream().filter(o -> !o.resolvida()).toList();
    }
}
