package pt.ulisboa.pedala.estacoes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DocaService {
    private final Map<String, Doca> docas = new ConcurrentHashMap<>();
    private final Map<String, Bicicleta> bicicletas = new ConcurrentHashMap<>();
    private final Map<String, Estacao> estacoes = new ConcurrentHashMap<>();

    public void registarEstacao(Estacao estacao) {
        estacoes.put(estacao.id(), estacao);
    }

    public void registarDoca(Doca doca) {
        docas.put(doca.getId(), doca);
    }

    public void registarBicicleta(Bicicleta bicicleta) {
        bicicletas.put(bicicleta.getId(), bicicleta);
    }

    public Optional<Doca> obterDoca(String docaId) {
        return Optional.ofNullable(docas.get(docaId));
    }

    public Optional<Bicicleta> obterBicicleta(String bicicletaId) {
        return Optional.ofNullable(bicicletas.get(bicicletaId));
    }

    public Optional<String> obterFreguesiaDaDoca(String docaId) {
        return obterDoca(docaId)
            .map(Doca::getEstacaoId)
            .map(estacoes::get)
            .map(Estacao::freguesia);
    }

    public EstadoDoca consultarEstado(String docaId) {
        return obterDoca(docaId).map(Doca::getEstado)
            .orElse(EstadoDoca.EM_MANUTENCAO);
    }

    public String retirarBicicletaParaViagem(String docaId) {
        Doca doca = obterDoca(docaId)
            .orElseThrow(() -> new IllegalArgumentException("Doca não encontrada: " + docaId));

        synchronized (doca) {
            if (doca.getEstado() != EstadoDoca.OCUPADA) {
                throw new IllegalStateException("A doca não tem uma bicicleta disponível.");
            }

            String bicicletaId = doca.getBicicletaId();
            Bicicleta bicicleta = obterBicicleta(bicicletaId)
                .orElseThrow(() -> new IllegalStateException("Bicicleta não encontrada: " + bicicletaId));

            synchronized (bicicleta) {
                if (bicicleta.getEstado() != EstadoBicicleta.DISPONIVEL) {
                    throw new IllegalStateException("A bicicleta não está disponível.");
                }

                doca.libertar();
                bicicleta.marcarEmViagem();
            }

            return bicicletaId;
        }
    }

    public void devolverBicicleta(String docaId, String bicicletaId) {
        Doca doca = obterDoca(docaId)
            .orElseThrow(() -> new IllegalArgumentException("Doca não encontrada: " + docaId));

        Bicicleta bicicleta = obterBicicleta(bicicletaId)
            .orElseThrow(() -> new IllegalArgumentException("Bicicleta não encontrada: " + bicicletaId));

        synchronized (doca) {
            synchronized (bicicleta) {
                if (doca.getEstado() != EstadoDoca.LIVRE) {
                    throw new IllegalStateException("A doca de destino não está livre.");
                }

                if (bicicleta.getEstado() != EstadoBicicleta.EM_VIAGEM) {
                    throw new IllegalStateException("A bicicleta não está em viagem.");
                }

                doca.docarBicicleta(bicicletaId);
                bicicleta.disponibilizar();
            }
        }
    }

    public void bloquearDoca(String docaId) {
        obterDoca(docaId)
            .orElseThrow(() -> new IllegalArgumentException("Doca não encontrada: " + docaId))
            .bloquearPorAvaria();
    }

    public void bloquearBicicleta(String bicicletaId) {
        obterBicicleta(bicicletaId)
            .orElseThrow(() -> new IllegalArgumentException("Bicicleta não encontrada: " + bicicletaId))
            .marcarComoAvariada();
    }

    public void libertarDocaAposReparacao(String docaId) {
        obterDoca(docaId)
            .orElseThrow(() -> new IllegalArgumentException("Doca não encontrada: " + docaId))
            .libertar();
    }
}