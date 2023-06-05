package ave.avesanties.arld.dto;

import java.time.LocalDateTime;

public class ArticleBufferEntry {
  
  private String title;
  
  private String newsSite;
  
  private LocalDateTime publishedAt;
  
  private String url;
  
  private String text;
  
  public ArticleBufferEntry() {
    
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

  public void setText(String text) {
    this.text = text;
  }
}
