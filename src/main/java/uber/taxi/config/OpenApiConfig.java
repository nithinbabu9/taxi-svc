package uber.taxi.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OpenApiConfig {

    @Bean
    OpenAPI taxiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Taxi Ride Matching API")
                        .version("1.0.0-MVP")
                        .description("""
                                REST API for registering users, creating ride requests, discovering route-compatible
                                travelers, completing two-sided match acceptance, and messaging in the private
                                chat created for an accepted match.

                                Typical workflow:
                                1. Register or log in two users to obtain bearer tokens.
                                2. Create one future ride for each user. Matching runs when a ride is created.
                                3. Retrieve a ride's potential matches.
                                4. Both participating users accept the same match.
                                5. Use the returned chat ID to send and retrieve messages.

                                Except for registration and login, endpoints require a JWT bearer token. The token
                                subject determines the acting user; user IDs are never accepted from request bodies.
                                """)
                        .contact(new Contact().name("Taxi API maintainers"))
                        .license(new License().name("Private project")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local development")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register and log in to receive JWTs."),
                        new Tag().name("Users").description("Retrieve the authenticated platform user."),
                        new Tag().name("Rides").description("Create, inspect, update, and cancel planned trips."),
                        new Tag().name("Matches").description("Inspect compatibility results and record user decisions."),
                        new Tag().name("Chats").description("Access accepted-match chats and exchange messages.")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repository setup and workflow documentation")
                        .url("./README.md"));
    }
}
