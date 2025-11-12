import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    jacoco // 테스트 커버리지 측정
}

group = "io.realfds"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot WebFlux (비동기 웹 프레임워크)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring Data R2DBC (비동기 데이터베이스 액세스)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    // PostgreSQL R2DBC Driver (PostgreSQL 비동기 드라이버)
    implementation("org.postgresql:r2dbc-postgresql:1.0.3.RELEASE")

    // Flyway (데이터베이스 마이그레이션)
    implementation("org.flywaydb:flyway-core:10.22.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.22.0")

    // PostgreSQL JDBC Driver (Flyway가 사용)
    runtimeOnly("org.postgresql:postgresql:42.7.1")

    // Spring Boot Actuator (헬스 체크, 메트릭)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Boot WebSocket (실시간 메트릭 브로드캐스트용)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Spring WebClient는 이미 spring-boot-starter-webflux에 포함되어 있음 (비동기 HTTP 클라이언트)

    // Micrometer (메트릭 수집)
    implementation("io.micrometer:micrometer-core")

    // Kafka (알림 이벤트 수신)
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kafka:reactor-kafka")

    // Jackson Kotlin Module (JSON 직렬화/역직렬화)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Reactor (리액티브 스트림)
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Coroutines (코루틴 지원)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Validation (데이터 검증)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Logback (로깅)
    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4") // JSON 로깅

    // Lombok (Java 클래스용 - @Slf4j 등)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 개발 도구
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // 테스트 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.projectreactor:reactor-test") // Reactor 테스트
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // Mockito Kotlin DSL

    // Testcontainers (통합 테스트용 PostgreSQL 컨테이너)
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")

    // WebTestClient (API 테스트)
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")

    // AssertJ (테스트 Assertion)
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict" // Null 안전성 강화
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform() // JUnit 5 사용
}

// JaCoCo 테스트 커버리지 설정 (Constitution V: ≥70% 커버리지 요구)
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // 테스트 실행 후 리포트 생성

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                // 제외할 클래스 (Config, DTO, Entity 등)
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/domain/**"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal() // 최소 70% 커버리지
            }
        }
    }
}

// 빌드 시 커버리지 검증 실행
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Spring Boot 애플리케이션 설정
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("alert-dashboard.jar")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}
