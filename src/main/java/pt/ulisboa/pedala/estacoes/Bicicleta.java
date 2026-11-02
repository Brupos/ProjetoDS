package pt.ulisboa.pedala.estacoes;

public class Bicicleta {
    private final String id;
    private EstadoBicicleta estado;
    private int nivelBateria;

    public Bicicleta(String id, EstadoBicicleta estado, int nivelBateria) {
        this.id = id;
        this.estado = estado;
        this.nivelBateria = nivelBateria;
    }

    public String getId() { return id; }
    public EstadoBicicleta getEstado() { return estado; }
    public int getNivelBateria() { return nivelBateria; }

    public void marcarComoAvariada() {
        this.estado = EstadoBicicleta.AVARIADA;
    }

    public void colocarEmReparacao() {
        this.estado = EstadoBicicleta.EM_MANUTENCAO;
    }

    public void disponibilizar() {
        this.estado = EstadoBicicleta.DISPONIVEL;
    }
}
