package ave.avesanties.arld.config;

import ave.avesanties.arld.controller.LoaderController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Class runs worker threads.
 */
@Component
public class WorkersRunner implements ApplicationRunner {

  private final LoaderController loaderController;
  private final ThreadPoolTaskExecutor taskExecutor;

  public WorkersRunner(LoaderController loaderController, ThreadPoolTaskExecutor taskExecutor) {
    this.loaderController = loaderController;
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void run(ApplicationArguments args) {
    final int pool = taskExecutor.getMaxPoolSize();
    for (int i = 0; i < pool; i++) {
      taskExecutor.execute(loaderController::load);
    }
  }

}
