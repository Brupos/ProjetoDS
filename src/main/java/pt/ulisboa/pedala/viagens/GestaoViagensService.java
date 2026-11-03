
package pt.ulisboa.pedala.viagens;

import pt.ulisboa.pedala.estacoes.DocaService;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GestaoViagensService {

    private final DocaService docaService;
    private final AutorizadorViagem autorizador;
    private final PublicadorViagemConcluida publicador;

    private final Map<String, Viagem> viagens = new ConcurrentHashMap<>();

    public GestaoViagensService(
            DocaService docaService,
            AutorizadorViagem autorizador,
            PublicadorViagemConcluida publicador) {
        this.docaService = docaService;
        this.autorizador = autorizador;
        this.publicador = publicador;
    }

    public synchronized Viagem iniciarViagem(
            String utilizadorId,
            String docaOrigemId,
            Instant dataInicio) {

        if (utilizadorId == null || utilizadorId.isBlank()) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }

        if (dataInicio == null) {
            throw new IllegalArgumentException("Data de início inválida.");
        }

        if (!autorizador.utilizadorPodeIniciar(utilizadorId)) {
            throw new IllegalStateException(
                    "Utilizador não autorizado a iniciar viagem.");
        }

        String bicicletaId =
                docaService.retirarBicicletaParaViagem(docaOrigemId);

        Viagem viagem = new Viagem(
                UUID.randomUUID().toString(),
                utilizadorId,
                bicicletaId,
                docaOrigemId,
                dataInicio,
                null
        );

        viagens.put(viagem.id(), viagem);
        return viagem;
    }

    public void registarViagem(Viagem viagem) {
        viagens.put(viagem.id(), viagem);
    }

    public Optional<Viagem> obterViagem(String viagemId) {
        return Optional.ofNullable(viagens.get(viagemId));
    }

    public synchronized ViagemConcluidaEvent terminarViagem(
            String viagemId,
            String docaDestinoId,
            Instant dataFim) {

        Viagem viagem = obterViagem(viagemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Viagem não encontrada: " + viagemId));

        if (viagem.fim() != null) {
            throw new IllegalStateException("A viagem já foi concluída.");
        }

        if (dataFim == null || dataFim.isBefore(viagem.inicio())) {
            throw new IllegalArgumentException(
                    "A data de fim não pode ser anterior ao início.");
        }

        docaService.devolverBicicleta(
                docaDestinoId, viagem.bicicletaId());

        Viagem concluida = new Viagem(
                viagem.id(),
                viagem.utilizadorId(),
                viagem.bicicletaId(),
                viagem.docaOrigemId(),
                viagem.inicio(),
                dataFim
        );

        viagens.put(viagemId, concluida);

        long duracao = Duration.between(
                viagem.inicio(), dataFim
        ).toMinutes();

        ViagemConcluidaEvent evento = new ViagemConcluidaEvent(
                concluida.id(),
                concluida.utilizadorId(),
                concluida.docaOrigemId(),
                docaDestinoId,
                concluida.inicio(),
                concluida.fim(),
                duracao
        );

        publicador.publicar(evento);
        return evento;
    }
}
