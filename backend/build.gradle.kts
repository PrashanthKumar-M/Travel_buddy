plugins {
    java
    application
    id("io.spring.dependency-management") version "1.1.6"
}

repositories {
    mavenCentral()
}

group = "com.travelbuddy"
version = "1.0.0"

application {
    mainClass.set("com.travelbuddy.api.TravelBuddyBackendApplication")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
