dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation("net.dv8tion:JDA:5.0.0-alpha.22")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")

    implementation("org.jetbrains:annotations:23.0.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}