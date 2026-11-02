package pt.ulisboa.pedala.estacoes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DocaService {
    private final Map<String, Doca> docas = new ConcurrentHashMap<>();
    private final Map<String, Bicicleta> bicicletas = new ConcurrentHashMap<>();

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

    public EstadoDoca consultarEstado(String docaId) {
        return obterDoca(docaId).map(Doca::getEstado).orElse(EstadoDoca.EM_MANUTENCAO);
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
