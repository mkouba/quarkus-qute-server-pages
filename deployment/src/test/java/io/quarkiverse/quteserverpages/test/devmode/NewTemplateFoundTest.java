package io.quarkiverse.quteserverpages.test.devmode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class NewTemplateFoundTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClasses(Foo.class)
                    .addAsResource(new StringAsset(
                            "We need a dummy template otherwise QuarkusDevModeTest#addResourceFile() does not work!"),
                            "templates/dummy.html"));

    @Test
    public void testNewTemplateFound() {
        given()
                .when().get("/qsp/hello")
                .then()
                .statusCode(404);

        test.addResourceFile("templates/hello.html", "Hello {cdi:foo.ping}!");

        given()
                .when().get("/qsp/hello")
                .then()
                .statusCode(200)
                .body(containsString("Hello Foo!"));
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        public String ping() {
            return "Foo";
        }

    }
}
