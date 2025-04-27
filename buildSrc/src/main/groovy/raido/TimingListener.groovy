package raido

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

import java.util.concurrent.TimeUnit

/** Log timings per task.
 Implemented so we can keep an eye on which tasks take a long time in the 
 Github workflow actions.
 
 https://stackoverflow.com/a/42793367/924597
 */
class TimingListener implements TaskExecutionListener, BuildListener {
  private long startTime
  private timings = []

  @Override
  void beforeExecute(Task task) {
    startTime = System.nanoTime()
  }

  @Override
  void afterExecute(Task task, TaskState taskState) {
    def ms = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    timings.add([ms, task.path])
    task.project.logger.warn "${task.path} took ${ms}ms"
  }

  @Override
  void buildFinished(BuildResult result) {
    println "Task timings:"
    for( timing in timings ){
      if( timing[0] >= 50 ){
        printf "%7sms  %s\n", timing
      }
    }
  }

  @Override
  void projectsEvaluated(Gradle gradle) {}

  @Override
  void projectsLoaded(Gradle gradle) {}

  @Override
  void settingsEvaluated(Settings settings) {}
}

