package uber.taxi.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!local")
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class GoogleMapsConfig {

    @Bean("googleGeocodingRestClient")
    RestClient googleGeocodingRestClient(RestClient.Builder builder, GoogleMapsProperties properties) {
        return createClient(builder, properties.geocodingBaseUrl().toString(), properties);
    }

    @Bean("googleRoutesRestClient")
    RestClient googleRoutesRestClient(RestClient.Builder builder, GoogleMapsProperties properties) {
        return createClient(builder, properties.routesBaseUrl().toString(), properties);
    }

    private RestClient createClient(RestClient.Builder builder, String baseUrl, GoogleMapsProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toMillis(properties.connectTimeout()));
        requestFactory.setReadTimeout(toMillis(properties.readTimeout()));
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    private int toMillis(Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }
}
