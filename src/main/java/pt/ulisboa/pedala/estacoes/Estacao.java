package pt.ulisboa.pedala.estacoes;

public record Estacao(String id, String freguesia) {
    public Estacao {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id da estação não pode ser nulo ou vazio");
        }
        if (freguesia == null || freguesia.isBlank()) {
            throw new IllegalArgumentException("freguesia não pode ser nula ou vazia");
        }
    }
}
