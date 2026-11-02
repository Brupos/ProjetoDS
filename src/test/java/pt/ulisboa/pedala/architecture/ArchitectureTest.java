package pt.ulisboa.pedala.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "pt.ulisboa.pedala", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule sem_ciclos_entre_pacotes =
        slices().matching("pt.ulisboa.pedala.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule relatorios_nao_deve_aceder_a_faturacao =
        noClasses().that().resideInAPackage("..relatorios..")
            .should().accessClassesThat().resideInAPackage("..faturacao..");

    @ArchTest
    public static final ArchRule faturacao_nao_deve_aceder_a_estacoes =
        noClasses().that().resideInAPackage("..faturacao..")
            .should().accessClassesThat().resideInAPackage("..estacoes..");
}
