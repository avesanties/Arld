package ave.avesanties.arld.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ave.avesanties.arld.controller.LoaderController;

@Component
public class WorkersRunner implements ApplicationRunner {

  @Autowired
  private LoaderController loaderController;

  @Autowired
  private ThreadPoolTaskExecutor taskExecutor;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    final int pool = taskExecutor.getMaxPoolSize();
    for (int i = 0; i < pool; i++) {
      taskExecutor.execute(() -> {
        loaderController.load();
      });
    }
  }

}
