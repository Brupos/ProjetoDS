package pt.ulisboa.pedala.relatorios;

import pt.ulisboa.pedala.estacoes.DocaService;

public class RelatorioUtilizacaoService {
    private final DocaService docaService;

    public RelatorioUtilizacaoService(DocaService docaService) {
        this.docaService = docaService;
    }

    public long obterTotalDocasBloqueadas(Iterable<String> docasIds) {
        long contagem = 0;
        for (String id : docasIds) {
            if (docaService.consultarEstado(id).name().contains("BLOQUEADA")) {
                contagem++;
            }
        }
        return contagem;
    }
}
