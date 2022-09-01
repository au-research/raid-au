package raido.apisvc.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import raido.apisvc.util.JvmUtil;
import raido.apisvc.util.Log;

import java.time.LocalDateTime;

import static raido.apisvc.util.Log.to;

@Component
public class StartupListener implements
  ApplicationListener<ContextRefreshedEvent> {
  
  private static final Log log = to(StartupListener.class);

  @Value("${raido.greeting:no greeting config}")
  private String greeting;
 
  private LocalDateTime startTime;
  
  @Override public void onApplicationEvent(ContextRefreshedEvent event) {
    log.info("%s - %s", event.getSource().toString(), greeting);
    JvmUtil.logStartupInfo();
    this.startTime = LocalDateTime.now();
  }

  /**
   Acutally "ContextRefreshed" that "Start" time, but close enough for now.
   */
  public LocalDateTime getStartTime() {
    return startTime;
  }
}