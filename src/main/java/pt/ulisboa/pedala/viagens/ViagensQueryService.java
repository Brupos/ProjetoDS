package pt.ulisboa.pedala.viagens;

import pt.ulisboa.pedala.estacoes.DocaService;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ViagensQueryService {
    private final DocaService docaService;
    private final ConcurrentMap<String, Viagem> viagensConcluidas = new ConcurrentHashMap<>();

    public ViagensQueryService(DocaService docaService) {
        this.docaService = Objects.requireNonNull(docaService, "docaService não pode ser nulo");
    }

    public void registarViagemConcluida(Viagem viagem) {
        Objects.requireNonNull(viagem, "viagem não pode ser nula");
        validarTexto(viagem.id(), "id da viagem");
        validarTexto(viagem.docaOrigemId(), "doca de origem");
        Objects.requireNonNull(viagem.inicio(), "início da viagem não pode ser nulo");
        if (viagem.fim() == null || viagem.fim().isBefore(viagem.inicio())) {
            throw new IllegalArgumentException("A viagem tem de estar concluída com fim após o início");
        }

        Viagem existente = viagensConcluidas.putIfAbsent(viagem.id(), viagem);
        if (existente != null && !existente.equals(viagem)) {
            throw new IllegalArgumentException(
                "id de viagem já registado com conteúdo diferente: " + viagem.id()
            );
        }
    }

    public List<ViagemAgregadaPorFreguesiaHora> agregarPartidasPorFreguesiaEHora(YearMonth mes) {
        Objects.requireNonNull(mes, "mês não pode ser nulo");
        Instant inicioMes = mes.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant inicioMesSeguinte = mes.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var totais = new java.util.HashMap<ChaveAgregacao, Long>();

        for (Viagem viagem : viagensConcluidas.values()) {
            if (viagem.inicio().isBefore(inicioMes) || !viagem.inicio().isBefore(inicioMesSeguinte)) {
                continue;
            }

            String freguesia = docaService.obterFreguesiaDaDoca(viagem.docaOrigemId())
                .orElseThrow(() -> new IllegalStateException(
                    "Não foi possível determinar a freguesia da doca: " + viagem.docaOrigemId()
                ));
            Instant horaUtc = viagem.inicio().truncatedTo(ChronoUnit.HOURS);
            totais.merge(new ChaveAgregacao(freguesia, horaUtc), 1L, Long::sum);
        }

        List<ViagemAgregadaPorFreguesiaHora> resultado = new ArrayList<>();
        totais.forEach((chave, total) -> resultado.add(
            new ViagemAgregadaPorFreguesiaHora(chave.freguesia(), chave.horaUtc(), total)
        ));
        resultado.sort(Comparator.comparing(ViagemAgregadaPorFreguesiaHora::freguesia)
            .thenComparing(ViagemAgregadaPorFreguesiaHora::horaUtc));
        return List.copyOf(resultado);
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser nulo ou vazio");
        }
    }

    private record ChaveAgregacao(String freguesia, Instant horaUtc) {}
}
