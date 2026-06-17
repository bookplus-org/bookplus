package com.bookplus.inventory.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Tests de arquitectura (ArchUnit): fronteras hexagonales + DDD como reglas verificables.
 * Solo se analizan clases de producción (sin tests).
 *
 * Excepción deliberada: los puertos del dominio pueden usar los tipos de paginación de
 * Spring Data (Page/Pageable). Es un patrón pragmático aceptado; el resto de Spring sí
 * queda prohibido en el dominio.
 */
@AnalyzeClasses(packages = "com.bookplus.inventory", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    /** Spring, excepto Spring Data (permitido en puertos para paginación). */
    private static final DescribedPredicate<JavaClass> SPRING_EXCEPTO_DATA =
            resideInAnyPackage("org.springframework..")
                    .and(DescribedPredicate.not(resideInAnyPackage("org.springframework.data..")));

    @ArchTest
    static final ArchRule capas_respetan_la_direccion_de_dependencias =
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Adapter").definedBy("..adapter..")
                    .layer("Config").definedBy("..config..")
                    .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Config")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_spring_salvo_spring_data =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat(SPRING_EXCEPTO_DATA)
                    .as("El dominio libre de Spring, salvo los tipos de paginación de Spring Data");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
                    .as("El dominio no debe conocer detalles de persistencia (JPA)");

    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_adaptadores =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("Los casos de uso dependen de puertos, no de adaptadores");

    @ArchTest
    static final ArchRule los_casos_de_uso_estan_en_application_usecase =
            classes().that().areAnnotatedWith(com.bookplus.inventory.shared.annotation.UseCase.class)
                    .should().resideInAPackage("..application.usecase..")
                    .allowEmptyShould(true)
                    .as("Las clases @UseCase viven en application.usecase");

    @ArchTest
    static final ArchRule las_entidades_jpa_estan_en_persistence =
            classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().resideInAPackage("..adapter.out.persistence.entity..")
                    .allowEmptyShould(true)
                    .as("Las @Entity de JPA viven en el adaptador de persistencia");
}
