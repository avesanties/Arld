package ave.avesanties.arld.controller;

import ave.avesanties.arld.dto.ArticleBufferEntry;
import ave.avesanties.arld.dto.Mapper;
import ave.avesanties.arld.model.Article;
import ave.avesanties.arld.service.ArticleService;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LoaderController {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoaderController.class);

  private final Mapper mapper;

  private final ArticleService arService;

  private final WebClient webClient;

  @Value("${loaderController.stop-words}")
  private String stopWords;

  @Value("${loaderController.limit-per-site-in-buffer}")
  private int limitPerSite;

  @Value("${loaderController.articles-required}")
  private int arRequired;

  @Value("${loaderController.articles-per-request}")
  private int step;

  @Value("${loaderController.api}")
  private String baseUrl;

  private final AtomicLong nextStep = new AtomicLong(0);

  private final AtomicInteger arCounter = new AtomicInteger(0);

  private volatile int arInDb = 0;

  private final Map<String, ArrayBlockingQueue<ArticleBufferEntry>> buffer =
      new ConcurrentHashMap<>();

  private final Set<String> stopWordsSet = new HashSet<>();

  private String filterPattern;

  public LoaderController(Mapper mapper, ArticleService arService, WebClient webClient) {
    this.mapper = mapper;
    this.arService = arService;
    this.webClient = webClient;
  }

  @PostConstruct
  void init() {
    stopWordsSet.addAll(Arrays.stream(stopWords.split(",")).collect(Collectors.toSet()));
    filterPattern = "((^|.*\\s)(" + String.join("|", stopWordsSet) + ")(\\s.*|$))";
  }

  public void load() {
    LOGGER.info("{} started", Thread.currentThread().getName());
    boolean workDone = false;

    while (!workDone) {
      // Get entries from api
      final List<ArticleBufferEntry> entries = downloadEntries();
      LOGGER.info("{} entries downloaded: {}", Thread.currentThread().getName(), entries.size());
      if (entries.isEmpty()) {
        LOGGER.info("{} api can't provide any more articles. The thread will be stopped",
            Thread.currentThread().getName());
        break;
      }
      // Filter and group by newsSite entries
      final Map<String, List<ArticleBufferEntry>> groupedEntries = entries.stream().filter(e -> {
            String s = e.getTitle().toLowerCase();
            return !s.matches(filterPattern);
          })
          .sorted(Comparator.comparing(ArticleBufferEntry::getPublishedAt))
          .collect(Collectors.groupingBy(ArticleBufferEntry::getNewsSite));

      // Put entries into buffer
      for (Map.Entry<String, List<ArticleBufferEntry>> groupEntry : groupedEntries.entrySet()) {
        // Increase articles counter
        final int groupSize = groupEntry.getValue().size();
        arCounter.addAndGet(groupSize);
        LOGGER.info("{} counter: {}", Thread.currentThread().getName(), arCounter);

        final BlockingQueue<ArticleBufferEntry> group =
            buffer.compute(groupEntry.getKey(), (k, v) -> {
              v = v == null ? new ArrayBlockingQueue<>(500) : v;
              v.addAll(groupEntry.getValue());
              return v;
            });

        // Check weather amount of entries in current group exceeds limit
        if (group.size() > limitPerSite) {
          processBuffer(group);
        }
      }

      // Buffer to be processed?
      if (arCounter.intValue() >= arRequired) {
        buffer.values().forEach(this::processBuffer);
      }

      // if db and buffer contains enough entries then stop processing
      workDone = arCounter.intValue() >= arRequired;
    }

    LOGGER.info("{} finished work", Thread.currentThread().getName());
  }

  public List<ArticleBufferEntry> downloadEntries() {
    final Long from = nextStep.getAndAdd(step);
    final URI apiAddress = UriComponentsBuilder.fromHttpUrl(baseUrl).queryParam("_limit", step)
        .queryParam("_start", from).build().toUri();
    ResponseEntity<List<ArticleBufferEntry>> response =
        webClient.get().uri(apiAddress).accept(MediaType.APPLICATION_JSON).retrieve()
            .toEntityList(ArticleBufferEntry.class).block();

    assert response != null;
    return response.getBody();
  }

  public void processBuffer(BlockingQueue<ArticleBufferEntry> articles) {
    final List<ArticleBufferEntry> entriesToSave = new ArrayList<>();
    articles.drainTo(entriesToSave);
    saveArticles(downLoadArticles(entriesToSave));
  }

  public List<Article> downLoadArticles(List<ArticleBufferEntry> entries) {
    final List<Object> contents = fetchAllArticles(entries);
    LOGGER.info("{} all article content to download: {}\nsuccessful downloads: {}",
        Thread.currentThread().getName(), entries.size(), contents.size());

    // In case the thread managed to download fewer articles than entries were
    arCounter.addAndGet(contents.size() - entries.size());

    final List<Article> articles = new ArrayList<>();
    int i = 0;
    while (i < contents.size()) {
      final ArticleBufferEntry entry = (ArticleBufferEntry) contents.get(i);
      articles.add(mapper.toArticle(entry));
      i++;
    }

    return articles;
  }

  // Method is synchronized to make sure specified articles number is provided
  public synchronized void saveArticles(List<Article> articles) {
    final int size = articles.size();

    if (arInDb >= arRequired || size == 0) {
      return;
    }

    final int from = 0;
    final int left = arRequired - arInDb;
    final int to = Math.min(size, left);

    arService.saveAll(articles.subList(from, to));
    arInDb += to;
    LOGGER.info("{} articles in db: {}", Thread.currentThread().getName(), arInDb);
  }


  public Mono<Object> getArticle(ArticleBufferEntry entry) {
    URI uri;
    try {
      uri = new URI(entry.getUrl());
    } catch (URISyntaxException e) {
      LOGGER.info("Failed to parse url: {}\n{}", entry.getUrl(), e.getStackTrace());
      return Mono.empty();
    }

    return webClient.get().uri(uri).retrieve().bodyToMono(String.class)
        .onErrorResume(WebClientException.class, ex -> {
          LOGGER.info("Error while getting article from: {}", entry.getUrl());
          ex.printStackTrace();
          return Mono.empty();
        }).map(t -> {
          entry.setText(t);
          return entry;
        });
  }

  public List<Object> fetchAllArticles(List<ArticleBufferEntry> links) {
    return Flux.fromIterable(links).flatMap(this::getArticle).collectList().block();
  }
}
