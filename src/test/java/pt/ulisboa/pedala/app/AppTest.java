package pt.ulisboa.pedala.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @Test
    @DisplayName("App exercita US-1, US-2 e US-4 no mesmo fluxo")
    void deveExecutarAsTresUserStories() {
        String entradas = String.join("\n",
            "1", "UTILIZADOR-1", "D1",
            "2", "D2",
            "4", "B1", "D2", "Falha nos travões",
            "5", "Travões substituídos",
            "6", YearMonth.now().toString(),
            "0"
        ) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        App.run(
            new Scanner(entradas),
            new PrintStream(output, true, StandardCharsets.UTF_8)
        );

        String resultado = output.toString(StandardCharsets.UTF_8);
        assertAll(
            () -> assertTrue(resultado.contains("Viagem iniciada!")),
            () -> assertTrue(resultado.contains("Viagem terminada!")),
            () -> assertTrue(resultado.contains("[Faturação]")),
            () -> assertTrue(resultado.contains("Avaria registada. Ordem:")),
            () -> assertTrue(resultado.contains("D2 bloqueada")),
            () -> assertTrue(resultado.contains("Reparação concluída; equipamento libertado.")),
            () -> assertTrue(resultado.contains("Alvalade |")),
            () -> assertTrue(resultado.contains("1 partidas"))
        );
    }

    @Test
    @DisplayName("App processa relatório municipal sem divulgar identificadores da viagem")
    void relatorioExibeApenasAgregados() {
        String entradas = String.join("\n",
            "1", "UTILIZADOR-PRIVADO", "D1",
            "2", "D2",
            "6", YearMonth.now().toString(),
            "0"
        ) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        App.run(
            new Scanner(entradas),
            new PrintStream(output, true, StandardCharsets.UTF_8)
        );

        String resultado = output.toString(StandardCharsets.UTF_8);
        int inicioRelatorio = resultado.indexOf("Viagens por freguesia e hora UTC:");
        int fimRelatorio = resultado.indexOf("Aplicação encerrada.");
        String relatorio = resultado.substring(inicioRelatorio, fimRelatorio);
        assertTrue(relatorio.contains("Alvalade |"));
        assertTrue(!relatorio.contains("UTILIZADOR-PRIVADO"));
        assertTrue(!relatorio.contains("D1"));
        assertTrue(!relatorio.contains("D2"));
    }

    @Test
    @DisplayName("App apresenta o menu antes de bloquear à espera de entrada")
    void deveMostrarMenuAntesDeLerPrimeiraOpcao() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicBoolean menuVisivelAntesDaLeitura = new AtomicBoolean();
        Reader reader = new Reader() {
            private final StringReader entrada = new StringReader("0\n");
            private boolean primeiraLeitura = true;

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                if (primeiraLeitura) {
                    primeiraLeitura = false;
                    menuVisivelAntesDaLeitura.set(
                        output.toString(StandardCharsets.UTF_8).contains("Opção: ")
                    );
                }
                return entrada.read(buffer, offset, length);
            }

            @Override
            public void close() {
                entrada.close();
            }
        };

        App.run(
            new Scanner(reader),
            new PrintStream(output, true, StandardCharsets.UTF_8)
        );

        assertTrue(menuVisivelAntesDaLeitura.get());
    }
}
