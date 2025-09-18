plugins {
    id("java")
    id("application")
}

group = "com.payroc.interviews"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.payroc.interviews.LoadBalancerApplication")
}
