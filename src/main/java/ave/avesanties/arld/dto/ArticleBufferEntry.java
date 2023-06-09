package ave.avesanties.arld.dto;

import java.time.LocalDateTime;

/**
 * Class is used to store article entries in buffer.
 */
public class ArticleBufferEntry {

  private String title;

  private String newsSite;

  private LocalDateTime publishedAt;

  private String url;

  private String text;

  public ArticleBufferEntry() {
    //Object is created via reflection api using Gson lib
  }

  public String getTitle() {
    return title;
  }

  public String getNewsSite() {
    return newsSite;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public String getUrl() {
    return url;
  }

  public String getText() {
    return text;
  }

  /**
   * Sets article text when downloading them from external API.
   *
   * @param text article text from news site.
   */
  public void setText(String text) {
    this.text = text;
  }
}
