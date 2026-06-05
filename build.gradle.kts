plugins {
    java
    application
}

group = "com.heater"
version = "0.2.0"
description = "Data Center Heater Side Gig — job 1: cool GPUs; side gig: route exhaust to DAC, algae, and community heat"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.knowm.xchart:xchart:3.8.7")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.heater.App")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("generateFigures") {
    group = "documentation"
    description = "Run scalability sweeps, generate charts, and patch README"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.heater.analysis.FigureMain")
    workingDir = projectDir
}

tasks.register<JavaExec>("generateConvectionFigures") {
    group = "documentation"
    description = "Run speculative convection DAC sweeps, charts, and patch README"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.heater.analysis.ConvectionFigureMain")
    workingDir = projectDir
}

tasks.register<JavaExec>("generateAcousticFigures") {
    group = "documentation"
    description = "Run speculative acoustic side-gig sweeps, charts, audio, and patch README"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.heater.analysis.AcousticFigureMain")
    workingDir = projectDir
}
