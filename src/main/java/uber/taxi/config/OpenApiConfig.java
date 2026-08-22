package uber.taxi.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI taxiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Taxi Ride Matching API")
                        .version("1.0.0-MVP")
                        .description("""
                                REST API for creating users and ride requests, discovering route-compatible
                                travelers, completing two-sided match acceptance, and messaging in the private
                                chat created for an accepted match.

                                Typical workflow:
                                1. Create two users.
                                2. Create one future ride for each user. Matching runs when a ride is created.
                                3. Retrieve a ride's potential matches.
                                4. Both participating users accept the same match.
                                5. Use the returned chat ID to send and retrieve messages.

                                Authentication is not implemented in this MVP. Acting user IDs are supplied in
                                request bodies and must belong to the relevant ride or chat.
                                """)
                        .contact(new Contact().name("Taxi API maintainers"))
                        .license(new License().name("Private project")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local development")))
                .tags(List.of(
                        new Tag().name("Users").description("Create and retrieve platform users."),
                        new Tag().name("Rides").description("Create, inspect, update, and cancel planned trips."),
                        new Tag().name("Matches").description("Inspect compatibility results and record user decisions."),
                        new Tag().name("Chats").description("Access accepted-match chats and exchange messages.")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repository setup and workflow documentation")
                        .url("./README.md"));
    }
}
