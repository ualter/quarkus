package ujr.combat.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EchoResourceTest {

    @Test
    public void testEchoEndpoint() {
    	String uuid = UUID.randomUUID().toString();
        given()
          .pathParam("word", uuid)
          .when().get("/echo/{word}")
          .then()
             .statusCode(200)
             .body(instanceOf(String.class));
    }

}