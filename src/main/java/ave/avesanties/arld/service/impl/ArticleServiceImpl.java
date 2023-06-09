package ave.avesanties.arld.service.impl;

import ave.avesanties.arld.model.Article;
import ave.avesanties.arld.model.ArticleRepository;
import ave.avesanties.arld.service.ArticleService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Services articles from {@link ArticleRepository}.
 */
@Service
public class ArticleServiceImpl implements ArticleService {

  private final ArticleRepository articlesRepository;

  public ArticleServiceImpl(ArticleRepository articlesRepository) {
    this.articlesRepository = articlesRepository;
  }

  @Override
  public void saveAll(List<Article> articles) {
    articlesRepository.saveAllAndFlush(articles);
  }

  @Override
  public List<Article> getAll() {
    return articlesRepository.findAll();
  }

  @Override
  public Optional<Article> getById(Long id) {
    return articlesRepository.findById(id);
  }

  @Override
  public List<Article> getByNewsSite(String newsSite) {
    return articlesRepository.findByNewsSite(newsSite);
  }

  @Override
  public Long countAll() {
    return articlesRepository.count();
  }
}
