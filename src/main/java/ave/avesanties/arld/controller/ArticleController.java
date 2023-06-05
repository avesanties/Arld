package ave.avesanties.arld.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ave.avesanties.arld.model.Article;
import ave.avesanties.arld.service.ArticleService;

@RestController
@RequestMapping("/api/v1")
public class ArticleController {
  
  ArticleService articleService;

  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }
  
  @GetMapping("/articles/all")
  public List<Article> getAll(){
    return articleService.getAll();
  }
  
  @GetMapping("/articles/{id}")
  public Article getById(@PathVariable(name = "id") Long id) {
    Optional<Article> article = articleService.getById(id);
    return article.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
  
  @GetMapping("/articles")
  public List<Article> getByNewsSite(@RequestParam(name = "site") String site){
    return articleService.getByNewsSite(site);
  }
}
