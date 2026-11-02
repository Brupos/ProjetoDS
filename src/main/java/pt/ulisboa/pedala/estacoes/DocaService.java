package pt.ulisboa.pedala.estacoes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocaService {
    private final Map<String, EstadoDoca> docas = new ConcurrentHashMap<>();

    public void registarDoca(String docaId, EstadoDoca estadoInicial) {
        docas.put(docaId, estadoInicial);
    }

    public EstadoDoca consultarEstado(String docaId) {
        return docas.getOrDefault(docaId, EstadoDoca.EM_MANUTENCAO);
    }

    public void bloquearDoca(String docaId) {
        docas.put(docaId, EstadoDoca.BLOQUEADA_AVARIA);
    }

    public void libertarDocaAposReparacao(String docaId) {
        docas.put(docaId, EstadoDoca.LIVRE);
    }
}
