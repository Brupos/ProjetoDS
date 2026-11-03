package pt.ulisboa.pedala.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "pt.ulisboa.pedala", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule sem_ciclos_entre_pacotes =
        slices().matching("pt.ulisboa.pedala.(*)..")
            .should().beFreeOfCycles();

    // R1: Estações não depende de outros módulos de negócio.
    @ArchTest
    public static final ArchRule estacoes_nao_deve_depender_de_outros_modulos =
        dependeApenasDe("..estacoes..", "..estacoes..");

    // A matriz allowed-to-use permite manutencao -> viagens e manutencao -> estacoes.
    @ArchTest
    public static final ArchRule viagens_so_deve_depender_de_estacoes =
        dependeApenasDe("..viagens..", "..viagens..", "..estacoes..");

    @ArchTest
    public static final ArchRule manutencao_so_deve_depender_de_viagens_e_estacoes =
        dependeApenasDe("..manutencao..", "..manutencao..", "..viagens..", "..estacoes..");

    @ArchTest
    public static final ArchRule faturacao_so_deve_depender_de_viagens =
        dependeApenasDe("..faturacao..", "..faturacao..", "..viagens..");

    @ArchTest
    public static final ArchRule relatorios_so_deve_depender_de_viagens_e_estacoes =
        dependeApenasDe("..relatorios..", "..relatorios..", "..viagens..", "..estacoes..");

    @ArchTest
    public static final ArchRule modulos_de_dominio_nao_devem_depender_de_app =
        noClasses().that().resideInAnyPackage(
            "..viagens..",
            "..estacoes..",
            "..manutencao..",
            "..faturacao..",
            "..relatorios.."
        ).should().dependOnClassesThat().resideInAPackage("..app..");

    @ArchTest
    public static final ArchRule app_pode_integrar_os_modulos_de_dominio =
        dependeApenasDe(
            "..app..",
            "..app..",
            "..viagens..",
            "..estacoes..",
            "..manutencao..",
            "..faturacao..",
            "..relatorios.."
        );

    private static ArchRule dependeApenasDe(String pacoteOrigem, String... pacotesPermitidos) {
        String[] pacotesPermitidosComBibliotecas = new String[pacotesPermitidos.length + 2];
        System.arraycopy(
            pacotesPermitidos,
            0,
            pacotesPermitidosComBibliotecas,
            0,
            pacotesPermitidos.length
        );
        pacotesPermitidosComBibliotecas[pacotesPermitidos.length] = "java..";
        pacotesPermitidosComBibliotecas[pacotesPermitidos.length + 1] = "org..";

        return classes().that().resideInAPackage(pacoteOrigem)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(pacotesPermitidosComBibliotecas);
    }
}
