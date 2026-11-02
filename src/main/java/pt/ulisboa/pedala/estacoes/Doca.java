package pt.ulisboa.pedala.estacoes;

public class Doca {
    private final String id;
    private final String estacaoId;
    private EstadoDoca estado;
    private String bicicletaId;

    public Doca(String id, String estacaoId, EstadoDoca estado, String bicicletaId) {
        this.id = id;
        this.estacaoId = estacaoId;
        this.estado = estado;
        this.bicicletaId = bicicletaId;
    }

    public String getId() { return id; }
    public String getEstacaoId() { return estacaoId; }
    public EstadoDoca getEstado() { return estado; }
    public String getBicicletaId() { return bicicletaId; }

    public void bloquearPorAvaria() {
        this.estado = EstadoDoca.BLOQUEADA_AVARIA;
    }

    public void libertar() {
        this.estado = EstadoDoca.LIVRE;
        this.bicicletaId = null;
    }

    public void docarBicicleta(String bicicletaId) {
        this.bicicletaId = bicicletaId;
        this.estado = EstadoDoca.OCUPADA;
    }
}
