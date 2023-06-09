package ave.avesanties.arld.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * This class provides beans configuration.
 */
@Configuration
public class AppConfig {

  @Value("${taskExecutor.threads}")
  private int poolSize;

  @Value("${webClient.timeout}")
  private int timeout;

  @Primary
  @Bean(destroyMethod = "shutdown")
  ThreadPoolTaskExecutor taskExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setMaxPoolSize(poolSize);
    executor.setCorePoolSize(poolSize);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("LoaderThread-");
    executor.initialize();

    return executor;
  }

  @Bean
  WebClient webClient() {
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build();

    return WebClient.builder().exchangeStrategies(strategies)
        .clientConnector(new ReactorClientHttpConnector(
            HttpClient.create().responseTimeout(Duration.ofMillis(timeout))))
        .build();
  }

  @Bean
  static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}
