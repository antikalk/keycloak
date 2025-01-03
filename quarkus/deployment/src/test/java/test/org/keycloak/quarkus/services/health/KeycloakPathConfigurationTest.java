/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.org.keycloak.quarkus.services.health;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

class KeycloakPathConfigurationTest {

    @BeforeAll
    static void setUpAll() {
        System.setProperty("KC_CACHE", "local");
    }

    @AfterAll
    static void tearDownAll() {
        System.clearProperty("KC_CACHE");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = 9001;
    }

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                .addAsResource("keycloak.conf", "META-INF/keycloak.conf"))
            .overrideConfigKey("kc.http-relative-path","/auth")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.path", "/prom/metrics")
            .overrideConfigKey("quarkus.class-loading.removed-artifacts", "io.quarkus:quarkus-jdbc-oracle,io.quarkus:quarkus-jdbc-oracle-deployment"); // config works a bit odd in unit tests, so this is to ensure we exclude Oracle to avoid ClassNotFound ex

    @Test
    void testMetrics() {
        given().basePath("/")
                .when().get("prom/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    void testHealth() {
        given().basePath("/")
                .when().get("health")
                .then()
                // Health is available under `/auth/health` (see http-relative-path),
                .statusCode(404);

        given().basePath("/")
                .when().get("auth/health")
                .then()
                .statusCode(200);
    }

    @Test
    void testWrongMetricsEndpoints() {
        given().basePath("/")
                .when().get("metrics")
                .then()
                // Metrics is available under `/prom/metrics` (see non-application-root-path),
                // so /metrics should return 404.
                .statusCode(404);

        given().basePath("/")
                .when().get("auth/metrics")
                .then()
                // Metrics is available under `/prom/metrics` (see non-application-root-path),
                // so /auth/metrics should return 404.
                .statusCode(404);

        given().basePath("/")
                .when().get("q/metrics")
                .then()
                // Metrics is available under `/prom/metrics` (see non-application-root-path),
                // so /q/metrics should return 404.
                .statusCode(404);

    }

    @Test
    void testAuthEndpointAvailable() {

        given().basePath("/")
                .when().get("auth")
                .then()
                .statusCode(200);
    }

    @Test
    void testRootRedirect() {
        given().basePath("/").redirects().follow(false)
                .when().get("")
                .then()
                .statusCode(302)
                .header("Location", is("/auth"));

        given().basePath("/")
                .when().get("")
                .then()
                .statusCode(200);
    }
}
