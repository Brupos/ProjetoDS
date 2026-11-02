package pt.ulisboa.pedala.manutencao;

import pt.ulisboa.pedala.estacoes.DocaService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class ManutencaoService {
    private final DocaService docaService;
    private final ConcurrentMap<String, OrdemReparacao> ordens = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> ordemIds = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, EventoProcessado> eventosProcessados = new ConcurrentHashMap<>();

    public ManutencaoService(DocaService docaService) {
        this.docaService = docaService;
    }

    public OrdemReparacao processarEventoAvaria(AvariaReportadaEvent evento) {
        Objects.requireNonNull(evento, "evento não pode ser nulo");
        validarTexto(evento.eventoId(), "eventoId");
        Objects.requireNonNull(evento.timestamp(), "timestamp não pode ser nulo");

        EventoProcessado processado = eventosProcessados.compute(evento.eventoId(), (eventoId, anterior) -> {
            if (anterior != null) {
                if (!anterior.evento().equals(evento)) {
                    throw new IllegalArgumentException(
                        "eventoId já foi processado com conteúdo diferente: " + eventoId
                    );
                }
                return anterior;
            }

            OrdemReparacao ordem = criarOrdem(evento.bicicletaId(), evento.docaId(), evento.descricao());
            return new EventoProcessado(evento, ordem);
        });
        return processado.ordem();
    }

    public OrdemReparacao reportarAvaria(String bicicletaId, String docaId, String motivo) {
        return criarOrdem(bicicletaId, docaId, motivo);
    }

    private OrdemReparacao criarOrdem(String bicicletaId, String docaId, String motivo) {
        validarIdentificadores(bicicletaId, docaId);
        validarTexto(motivo, "motivo");

        if (docaId != null && docaService.obterDoca(docaId).isEmpty()) {
            throw new IllegalArgumentException("Doca não encontrada: " + docaId);
        }
        if (bicicletaId != null && docaService.obterBicicleta(bicicletaId).isEmpty()) {
            throw new IllegalArgumentException("Bicicleta não encontrada: " + bicicletaId);
        }

        if (docaId != null) {
            docaService.bloquearDoca(docaId);
        }
        if (bicicletaId != null) {
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
        ordens.put(ordem.id(), ordem);
        ordemIds.add(ordem.id());
        return ordem;
    }

    public Optional<OrdemReparacao> concluirReparacao(String ordemId, String relatorioTecnico) {
        validarTexto(ordemId, "ordemId");
        validarTexto(relatorioTecnico, "relatorioTecnico");

        AtomicReference<OrdemReparacao> resultado = new AtomicReference<>();
        ordens.computeIfPresent(ordemId, (id, atual) -> {
            if (atual.resolvida()) {
                resultado.set(atual);
                return atual;
            }

            OrdemReparacao resolvida = atual.concluir(relatorioTecnico);
            if (resolvida.docaId() != null) {
                docaService.libertarDocaAposReparacao(resolvida.docaId());
            }
            if (resolvida.bicicletaId() != null) {
                docaService.obterBicicleta(resolvida.bicicletaId())
                           .ifPresent(bicicleta -> bicicleta.disponibilizar());
            }
            resultado.set(resolvida);
            return resolvida;
        });
        return Optional.ofNullable(resultado.get());
    }

    public List<OrdemReparacao> listarOrdensPendentes() {
        return obterOrdens().stream().filter(o -> !o.resolvida()).toList();
    }

    public List<OrdemReparacao> listarTodasOrdens() {
        return obterOrdens();
    }

    private List<OrdemReparacao> obterOrdens() {
        return ordemIds.stream()
            .map(ordens::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private static void validarIdentificadores(String bicicletaId, String docaId) {
        if (bicicletaId == null && docaId == null) {
            throw new IllegalArgumentException("É necessário indicar uma bicicleta ou uma doca");
        }
        if (bicicletaId != null) {
            validarTexto(bicicletaId, "bicicletaId");
        }
        if (docaId != null) {
            validarTexto(docaId, "docaId");
        }
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser nulo ou vazio");
        }
    }

    private record EventoProcessado(AvariaReportadaEvent evento, OrdemReparacao ordem) {}
}
