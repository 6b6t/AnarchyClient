package net.blockhost.anarchyclient;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Package boundaries. Rules that the code breaks today are frozen: their existing violations are recorded in
 * {@code src/test/resources/archunit_store}, and only new ones fail.
 */
final class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("net.blockhost.anarchyclient");
    }

    /** Mixin classes are merged into Minecraft classes, so loading one directly crashes the game. */
    @Test
    void onlyMixinInterfacesAreUsedOutsideMixins() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..mixin..")
                .should()
                .dependOnClassesThat(resideInAPackage("..mixin..").and(not(INTERFACES)))
                .because("only accessor and invoker interfaces of mixins may be used from other code")
                .check(classes);
    }

    /** Modules hold the game logic; the menu reads them, not the other way round. */
    @Test
    void modulesDoNotDependOnTheMenu() {
        FreezingArchRule.freeze(noClasses()
                        .that()
                        .resideInAPackage("..module..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..ui.."))
                .check(classes);
    }

    /** Settings are plain values that modules and the menu both use. */
    @Test
    void settingsDoNotDependOnModulesOrTheMenu() {
        noClasses()
                .that()
                .resideInAPackage("..setting..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..module..", "..ui..")
                .check(classes);
    }

    @Test
    void topLevelPackagesAreFreeOfCycles() {
        FreezingArchRule.freeze(slices().matching("net.blockhost.anarchyclient.(*)..")
                        .should()
                        .beFreeOfCycles())
                .check(classes);
    }
}
