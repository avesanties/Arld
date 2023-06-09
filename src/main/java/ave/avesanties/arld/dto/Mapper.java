package ave.avesanties.arld.dto;

import ave.avesanties.arld.model.Article;
import org.springframework.stereotype.Component;

/**
 * Object maps {@link ArticleBufferEntry} object to {@link Article} one.
 */
@Component
public class Mapper {

  public Article toArticle(ArticleBufferEntry dto) {
    return new Article(dto.getTitle(), dto.getNewsSite(), dto.getPublishedAt(), dto.getText());
  }
}
