package ave.avesanties.arld.service;

import java.util.List;
import java.util.Optional;
import ave.avesanties.arld.model.Article;

public interface ArticleService {

  void saveAll(List<Article> articles);

  List<Article> getAll();

  Optional<Article> getById(Long id);

  List<Article> getByNewsSite(String newsSite);
  
  Long countAll();
}
