package ave.avesanties.arld.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Article} repository.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

  @Transactional(readOnly = true)
  List<Article> findByNewsSite(String newsSite);
}
