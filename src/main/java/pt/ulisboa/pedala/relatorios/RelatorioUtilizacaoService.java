package pt.ulisboa.pedala.relatorios;

import pt.ulisboa.pedala.estacoes.DocaService;
import pt.ulisboa.pedala.viagens.ViagemAgregadaPorFreguesiaHora;
import pt.ulisboa.pedala.viagens.ViagensQueryService;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class RelatorioUtilizacaoService {
    private final DocaService docaService;
    private final ViagensQueryService viagensQueryService;

    public RelatorioUtilizacaoService(DocaService docaService, ViagensQueryService viagensQueryService) {
        this.docaService = Objects.requireNonNull(docaService, "docaService não pode ser nulo");
        this.viagensQueryService = Objects.requireNonNull(
            viagensQueryService,
            "viagensQueryService não pode ser nulo"
        );
    }

    public List<RelatorioFreguesiaHora> gerarRelatorioMensal(YearMonth mes) {
        return viagensQueryService.agregarPartidasPorFreguesiaEHora(mes).stream()
            .map(this::paraRelatorio)
            .toList();
    }

    private RelatorioFreguesiaHora paraRelatorio(ViagemAgregadaPorFreguesiaHora viagemAgregada) {
        return new RelatorioFreguesiaHora(
            viagemAgregada.freguesia(),
            viagemAgregada.horaUtc(),
            viagemAgregada.totalPartidas()
        );
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
