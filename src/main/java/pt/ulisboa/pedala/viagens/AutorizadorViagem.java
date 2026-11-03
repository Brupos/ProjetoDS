package pt.ulisboa.pedala.viagens;

public interface AutorizadorViagem {
    boolean utilizadorPodeIniciar(String utilizadorId);
}