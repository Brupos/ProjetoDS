package pt.ulisboa.pedala.faturacao;

import pt.ulisboa.pedala.viagens.ViagemConcluidaEvent;
import java.math.BigDecimal;

public class FaturaService {
    public BigDecimal calcularTarifa(ViagemConcluidaEvent evento) {
        if (evento.duracaoMinutos() <= 45) {
            return BigDecimal.ZERO;
        }
        long blocosExtras = (evento.duracaoMinutos() - 45 + 14) / 15;
        return BigDecimal.valueOf(blocosExtras).multiply(new BigDecimal("0.50"));
    }
}
