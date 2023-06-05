package ave.avesanties.arld.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "ARTICLES")
public class Article {

  @Id
  @SequenceGenerator(name = "speed_seq", initialValue = 1, allocationSize = 50)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "speed_seq")
  @Column(name = "id", nullable = false)
  private Long id;
  
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "title", nullable = false)
  private String title;
  
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "news_site", nullable = false)
  private String newsSite;
  
  @NotNull
  @PastOrPresent
  @Column(name = "published_date", nullable = false)
  private LocalDateTime publishedDate;
  
  @NotNull
  @Lob
  @Column(name = "article", nullable = false, columnDefinition = "CLOB NOT NULL")
  private String article;

  public Article() {

  }
  
  public Article(@NotNull @Size(min = 1, max = 255) String title,
      @NotNull @Size(min = 1, max = 255) String newsSite,
      @NotNull @PastOrPresent LocalDateTime publishedDate, @NotNull String article) {
    this.title = title;
    this.newsSite = newsSite;
    this.publishedDate = publishedDate;
    this.article = article;
  }
  
  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getNewsSite() {
    return newsSite;
  }

  public LocalDateTime getPublishedDate() {
    return publishedDate;
  }

  public String getArticle() {
    return article;
  }

  @Override
  public String toString() {
    return "Article [id=" + id + ", title=" + title + ", newsSite=" + newsSite + ", publishedDate="
        + publishedDate + ", article=" + article + "]";
  }

}
