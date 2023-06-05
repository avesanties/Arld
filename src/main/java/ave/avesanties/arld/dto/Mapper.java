package ave.avesanties.arld.dto;

import org.springframework.stereotype.Component;
import ave.avesanties.arld.model.Article;

@Component
public class Mapper {
  public Article toArticle(ArticleBufferEntry dto) {
    return new Article(dto.getTitle(), dto.getNewsSite(), dto.getPublishedAt(), dto.getText());
  }
}
