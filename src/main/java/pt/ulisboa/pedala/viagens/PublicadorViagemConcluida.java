package pt.ulisboa.pedala.viagens;

public interface PublicadorViagemConcluida {
    void publicar(ViagemConcluidaEvent evento);
}