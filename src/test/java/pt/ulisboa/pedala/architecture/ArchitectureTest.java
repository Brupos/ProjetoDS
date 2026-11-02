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

    // R1: EstacoesDocas é o núcleo mais estável (I = 0.00) - não depende de nenhum módulo de negócio
    @ArchTest
    public static final ArchRule estacoes_nao_deve_depender_de_outros_modulos =
        classes().that().resideInAPackage("..estacoes..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "..estacoes..",
                "java..",
                "org.."
            );

    // R2: Quebra de ciclo formalizada no ADR-006 (Manutenção consome eventos e atua em Estações)
    @ArchTest
    public static final ArchRule manutencao_nao_deve_aceder_a_viagens =
        noClasses().that().resideInAPackage("..manutencao..")
            .should().accessClassesThat().resideInAPackage("..viagens..");

    // R3: Isolamento RGPD / Fiscalidade (ADR-004 e 01-caracteristicas)
    @ArchTest
    public static final ArchRule relatorios_nao_deve_aceder_a_faturacao =
        noClasses().that().resideInAPackage("..relatorios..")
            .should().accessClassesThat().resideInAPackage("..faturacao..");

    // R4: Faturação não acede ao hardware nem à topologia física
    @ArchTest
    public static final ArchRule faturacao_nao_deve_aceder_a_estacoes =
        noClasses().that().resideInAPackage("..faturacao..")
            .should().accessClassesThat().resideInAPackage("..estacoes..");

    // R5: Viagens não acede a manutenção (comunicação desacoplada)
    @ArchTest
    public static final ArchRule viagens_nao_deve_aceder_a_manutencao =
        noClasses().that().resideInAPackage("..viagens..")
            .should().accessClassesThat().resideInAPackage("..manutencao..");
}
