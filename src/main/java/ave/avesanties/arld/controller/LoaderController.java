package ave.avesanties.arld.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ave.avesanties.arld.dto.ArticleBufferEntry;
import ave.avesanties.arld.dto.Mapper;
import ave.avesanties.arld.model.Article;
import ave.avesanties.arld.service.ArticleService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LoaderController {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoaderController.class);

  @Autowired
  private Mapper mapper;

  @Autowired
  ArticleService articleService;

  @Autowired
  private WebClient webClient;

  @Value("${loaderController.stop-words}")
  private String stopWords;

  @Value("${loaderController.limit-per-site-in-buffer}")
  private int LIMIT_PER_SITE;

  @Value("${loaderController.articles-required}")
  private int ARTICLES_REQUIRED;

  @Value("${loaderController.articles-per-request}")
  private int STEP;

  @Value("${loaderController.api}")
  private String BASE_URL;

  private volatile static AtomicLong nextStep = new AtomicLong(0);

  private volatile static AtomicInteger arCounter = new AtomicInteger(0);

  private volatile static int articlesInDb = 0;

  private static final Map<String, ArrayBlockingQueue<ArticleBufferEntry>> BUFFER =
      new ConcurrentHashMap<>();

  private static final Set<String> STOP_WORDS = new HashSet<String>();

  private static String FILTER_PATTERN;

  @PostConstruct
  void init() {
    STOP_WORDS.addAll(Arrays.stream(stopWords.split(",")).collect(Collectors.toSet()));
    FILTER_PATTERN = "((^|.*\\s)(" + String.join("|", STOP_WORDS) + ")(\\s.*|$))";
  }

  public void load() {
    LOGGER.info(Thread.currentThread().getName() + " started.");
    boolean workDone = false;

    while (!workDone) {
      // Get entries from api
      final List<ArticleBufferEntry> entries = downloadEntries();
      LOGGER.info(Thread.currentThread().getName() + " entries downloaded: " + entries.size());

      // Filter and group by newsSite entries
      final Map<String, List<ArticleBufferEntry>> groupedEntries = entries.stream().filter(e -> {
        String s = e.getTitle().toLowerCase();
        return !s.matches(FILTER_PATTERN);
      }).sorted(Comparator.comparing(ArticleBufferEntry::getPublishedAt))
          .collect(Collectors.groupingBy(ArticleBufferEntry::getNewsSite));

      // Put entries into buffer
      for (Map.Entry<String, List<ArticleBufferEntry>> groupEntry : groupedEntries.entrySet()) {
        // Increase articles counter
        final int groupSize = groupEntry.getValue().size();
        arCounter.addAndGet(groupSize);
        LOGGER.info(Thread.currentThread().getName() + " counter: " + arCounter.toString());
        
        final BlockingQueue<ArticleBufferEntry> group =
            BUFFER.compute(groupEntry.getKey(), (k, v) -> {
              v = v == null ? new ArrayBlockingQueue<ArticleBufferEntry>(500) : v;
              v.addAll(groupEntry.getValue());
              return v;
            });

        // Check weather amount of entries in current group exceeds limit
        if (group.size() > LIMIT_PER_SITE) {
          processBuffer(group);
        }
      }

      // Buffer to be processed?
      if (arCounter.intValue() >= ARTICLES_REQUIRED) {
        BUFFER.values().forEach(this::processBuffer);
      }

      // if db and buffer contains enough entries then stop processing
      workDone = arCounter.intValue() >= ARTICLES_REQUIRED;
    }

    LOGGER.info(Thread.currentThread().getName() + " finished work");
  }

  public List<ArticleBufferEntry> downloadEntries() {
    final Long from = nextStep.getAndAdd(STEP);
    final URI apiAddr = UriComponentsBuilder.fromHttpUrl(BASE_URL).queryParam("_limit", STEP)
        .queryParam("_start", from).build().toUri();
    ResponseEntity<List<ArticleBufferEntry>> response =
        webClient.get().uri(apiAddr).accept(MediaType.APPLICATION_JSON).retrieve()
            .toEntityList(ArticleBufferEntry.class).block();
    return response.getBody();
  }

  public void processBuffer(BlockingQueue<ArticleBufferEntry> group) {
    final List<ArticleBufferEntry> entriesToSave = new ArrayList<>();
    group.drainTo(entriesToSave);
    saveArticles(downLoadArticles(entriesToSave));
  }

  public List<Article> downLoadArticles(List<ArticleBufferEntry> entries) {
    final List<Object> contents = fetchArticles(entries);
    LOGGER.info(Thread.currentThread().getName() + "all article content to download: "
        + entries.size() + "\n" + "successful downloads: " + contents.size());

    // In case the thread managed to download less articles than entries were
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

    if (articlesInDb >= ARTICLES_REQUIRED || size == 0) {
      return;
    }

    final int from = 0;
    final int left = ARTICLES_REQUIRED - articlesInDb;
    int to = size > left ? left : size;

    articleService.saveAll(articles.subList(from, to));
    articlesInDb += to;
    LOGGER.info(Thread.currentThread().getName() + " articles in db: " + articlesInDb);
  }


  public Mono<Object> getArticle(ArticleBufferEntry entry) {
    URI uri = null;
    try {
      uri = new URI(entry.getUrl());
    } catch (URISyntaxException e) {
      LOGGER.info("Failed to parse url: " + entry.getUrl());
      e.printStackTrace();
      return Mono.empty();
    }

    return webClient.get().uri(uri).retrieve().bodyToMono(String.class)
        .onErrorResume(WebClientException.class, ex -> {
          LOGGER.info("Error while getting article from: " + entry.getUrl());
          ex.printStackTrace();
          return Mono.empty();
        }).map(t -> {
          entry.setText(t);
          return entry;
        });
  }

  public List<Object> fetchArticles(List<ArticleBufferEntry> links) {
    return Flux.fromIterable(links).flatMap(this::getArticle).collectList().block();
  }
}
