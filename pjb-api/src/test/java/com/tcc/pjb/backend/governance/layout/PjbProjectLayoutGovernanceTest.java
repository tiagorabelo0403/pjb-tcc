package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import org.junit.jupiter.api.Test;

class PjbProjectLayoutGovernanceTest {

    private static final Path WORKSPACE_ROOT = PjbTestPaths.projectRoot();
    private static final Path MODULE_ROOT = PjbTestPaths.projectRoot().resolve("pjb-api");
    private static final Path MAIN_JAVA = MODULE_ROOT.resolve("src/main/java");
    private static final Path MAIN_RESOURCES = MODULE_ROOT.resolve("src/main/resources");
    private static final Path TEST_JAVA = MODULE_ROOT.resolve("src/test/java");
    private static final Path TEST_RESOURCES = MODULE_ROOT.resolve("src/test/resources");
    private static final Path MODULE_POM = MODULE_ROOT.resolve("pom.xml");
    private static final Path ROOT_POM = WORKSPACE_ROOT.resolve("pom.xml");
    private static final Path CORE_MODULE = WORKSPACE_ROOT.resolve("pjb-core/src/main/java");
    private static final Path CORE_TEST_MODULE = WORKSPACE_ROOT.resolve("pjb-core/src/test/java");
    private static final Path GITIGNORE = WORKSPACE_ROOT.resolve(".gitignore");
    private static final Path WRAPPER_PROPS = WORKSPACE_ROOT.resolve(".mvn/wrapper/maven-wrapper.properties");
    private static final Path JVM_CONFIG = WORKSPACE_ROOT.resolve(".mvn/jvm.config");
    private static final Path MAVEN_CONFIG = WORKSPACE_ROOT.resolve(".mvn/maven.config");
    private static final Set<String> RESOURCE_EXTENSIONS = Set.of(".yml", ".yaml", ".properties", ".sql", ".xml", ".json", ".txt", ".md");
    private static final Set<String> BUILD_OUTPUT_DIRECTORIES = Set.of("target", "build", "out", "outcheck");

    @Test
    void estruturaCanonicaDoProjetoDeveExistir() {
        assertTrue(Files.isDirectory(CORE_MODULE));
        assertTrue(Files.isDirectory(CORE_TEST_MODULE));
        assertTrue(Files.isDirectory(MAIN_JAVA));
        assertTrue(Files.isDirectory(MAIN_RESOURCES));
        assertTrue(Files.isDirectory(TEST_JAVA));
        assertTrue(Files.isDirectory(TEST_RESOURCES));
        assertTrue(Files.isRegularFile(ROOT_POM));
        assertTrue(Files.isRegularFile(MODULE_POM));
        assertTrue(Files.isRegularFile(WORKSPACE_ROOT.resolve("mvnw")));
        assertTrue(Files.isRegularFile(WORKSPACE_ROOT.resolve("mvnw.cmd")));
        assertTrue(Files.isRegularFile(WRAPPER_PROPS));
        assertTrue(Files.isRegularFile(JVM_CONFIG));
        assertTrue(Files.isRegularFile(MAVEN_CONFIG));
        assertTrue(Files.isRegularFile(GITIGNORE));
    }

    @Test
    void pomDoWorkspaceEDoModuloDevemApontarParaTopologiaModularEJava21() throws IOException {
        String rootPom = Files.readString(ROOT_POM);
        assertTrue(rootPom.contains("<packaging>pom</packaging>"));
        assertTrue(rootPom.contains("<module>pjb-core</module>"));
        assertTrue(rootPom.contains("<module>pjb-api</module>"));
        assertTrue(rootPom.contains("<java.version>21</java.version>"));
        assertTrue(rootPom.contains("<maven.compiler.release>21</maven.compiler.release>"));
        assertTrue(!rootPom.contains("<sourceDirectory>src/main/java</sourceDirectory>"));
        assertTrue(!rootPom.contains("<testSourceDirectory>src/test/java</testSourceDirectory>"));

        String modulePom = Files.readString(MODULE_POM);
        assertTrue(modulePom.contains("<artifactId>pjb-api</artifactId>"));
        assertTrue(modulePom.contains("<artifactId>pjb-core</artifactId>"));
        assertTrue(!modulePom.contains("../src/main/java"));
        assertTrue(modulePom.contains("spring-boot-maven-plugin"));

        String gitignore = Files.readString(GITIGNORE);
        assertTrue(gitignore.contains("*.class"));

        String wrapper = Files.readString(WRAPPER_PROPS);
        assertTrue(wrapper.contains("distributionUrl="));
        assertTrue(wrapper.contains("wrapperVersion="));

        String jvmConfig = Files.readString(JVM_CONFIG);
        assertTrue(jvmConfig.contains("-Dfile.encoding=UTF-8"));

        String mavenConfig = Files.readString(MAVEN_CONFIG);
        assertTrue(mavenConfig.contains("-Dmaven.compiler.release=21"));
        assertTrue(mavenConfig.contains("-Dfile.encoding=UTF-8"));
    }

    @Test
    void projetoNaoDeveConterJavaForaDosRootsCanonicosNemClassDentroDosRootsDeCodigo() throws IOException {
        try (Stream<Path> stream = Files.walk(WORKSPACE_ROOT)) {
            List<Path> javaOutsideCanonicalRoots = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isBuildOutput(path))
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(MAIN_JAVA) && !path.startsWith(TEST_JAVA) && !path.startsWith(CORE_MODULE) && !path.startsWith(CORE_TEST_MODULE))
                    .toList();
            assertTrue(javaOutsideCanonicalRoots.isEmpty(), () -> "Java fora dos roots canônicos: " + javaOutsideCanonicalRoots);
        }
        try (Stream<Path> stream = Files.walk(MODULE_ROOT.resolve("src"))) {
            List<Path> classInsideSrc = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .toList();
            assertTrue(classInsideSrc.isEmpty(), () -> ".class indevido dentro de pjb-api/src: " + classInsideSrc);
        }
    }

    @Test
    void javaEResourcesNaoDevemEstarMisturadosNosRootsDoModuloApi() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_RESOURCES)) {
            List<Path> javaInResources = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            assertTrue(javaInResources.isEmpty(), () -> "Java em pjb-api/src/main/resources: " + javaInResources);
        }
        try (Stream<Path> stream = Files.walk(TEST_RESOURCES)) {
            List<Path> javaInTestResources = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            assertTrue(javaInTestResources.isEmpty(), () -> "Java em pjb-api/src/test/resources: " + javaInTestResources);
        }
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            List<Path> resourceLikeInsideMainJava = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> RESOURCE_EXTENSIONS.stream().anyMatch(extension -> path.getFileName().toString().endsWith(extension)))
                    .toList();
            assertTrue(resourceLikeInsideMainJava.isEmpty(), () -> "Recursos indevidos em pjb-api/src/main/java: " + resourceLikeInsideMainJava);
        }
        try (Stream<Path> stream = Files.walk(TEST_JAVA)) {
            List<Path> resourceLikeInsideTestJava = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> RESOURCE_EXTENSIONS.stream().anyMatch(extension -> path.getFileName().toString().endsWith(extension)))
                    .toList();
            assertTrue(resourceLikeInsideTestJava.isEmpty(), () -> "Recursos indevidos em pjb-api/src/test/java: " + resourceLikeInsideTestJava);
        }
    }

    private static boolean isBuildOutput(Path path) {
        Path relative = WORKSPACE_ROOT.relativize(path);
        for (Path part : relative) {
            if (BUILD_OUTPUT_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }
}
