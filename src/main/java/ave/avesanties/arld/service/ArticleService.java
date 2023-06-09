package ave.avesanties.arld.service;

import ave.avesanties.arld.model.Article;
import ave.avesanties.arld.model.ArticleRepository;
import java.util.List;
import java.util.Optional;

/**
 * Describes service which interacts with {@link ArticleRepository}.
 */
public interface ArticleService {

  void saveAll(List<Article> articles);

  List<Article> getAll();

  Optional<Article> getById(Long id);

  List<Article> getByNewsSite(String newsSite);

  Long countAll();
}
