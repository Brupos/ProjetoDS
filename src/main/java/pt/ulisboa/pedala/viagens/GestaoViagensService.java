
package pt.ulisboa.pedala.viagens;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GestaoViagensService {

    private final Map<String, Viagem> viagens = new ConcurrentHashMap<>();

    public void registarViagem(Viagem viagem) {
        viagens.put(viagem.id(), viagem);
    }

    public Optional<Viagem> obterViagem(String viagemId) {
        return Optional.ofNullable(viagens.get(viagemId));
    }

    public ViagemConcluidaEvent terminarViagem(
            String viagemId,
            String docaDestinoId,
            Instant dataFim) {

        Viagem viagem = obterViagem(viagemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Viagem não encontrada: " + viagemId));

        if (viagem.fim() != null) {
            throw new IllegalStateException("A viagem já foi concluída.");
        }

        if (dataFim.isBefore(viagem.inicio())) {
            throw new IllegalArgumentException(
                    "A data de fim não pode ser anterior ao início.");
        }

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

        return new ViagemConcluidaEvent(
                concluida.id(),
                concluida.utilizadorId(),
                concluida.docaOrigemId(),
                docaDestinoId,
                concluida.inicio(),
                concluida.fim(),
                duracao
        );
    }
}
