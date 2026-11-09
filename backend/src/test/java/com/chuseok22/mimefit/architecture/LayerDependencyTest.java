package com.chuseok22.mimefit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.chuseok22.mimefit", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    // core는 application·infrastructure에 의존하지 않는다.
    @ArchTest
    static final ArchRule coreMustNotDependOnApplicationOrInfrastructure =
        noClasses().that().resideInAPackage("..core..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .allowEmptyShould(true);

    // application은 infrastructure를 직접 참조하지 않는다(외부 연동은 core의 port로).
    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructure =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    // 도메인(최상위 패키지) 간 순환 의존을 금지한다.
    @ArchTest
    static final ArchRule domainsMustBeFreeOfCycles =
        slices().matching("com.chuseok22.mimefit.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

}
