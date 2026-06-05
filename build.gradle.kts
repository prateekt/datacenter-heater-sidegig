plugins {
    java
    application
}

group = "com.heater"
version = "0.2.0"
description = "Data Center Heater Side Gig — GPU waste-heat simulation for DAC and algae CO₂ removal"

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
