package asl.seedscan.scanner;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.scanner.scanworker.ScanWorker;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Mock ScanManager for testing purposes
 */
public class ScanManagerMock extends ScanManager {

  private int numberTasksAdded = 0;

  private boolean executeTasks = false;

  /**
   * Stores tasks added for test analysis.
   */
  private Queue<ScanWorker> testQueue = new LinkedBlockingQueue<>();

  public ScanManagerMock(MetricDatabase database,
      MetaGenerator metaGenerator) {
    super(database, metaGenerator);
  }

  @Override
  public synchronized void addTask(ScanWorker task) {
    if (executeTasks) {
      super.addTask(task);
    }
    testQueue.offer(task);
    numberTasksAdded++;
  }

  public synchronized int getNumberTasksAdded() {
    return numberTasksAdded;
  }

  public synchronized Queue<ScanWorker> getWorkQueue() {
    return testQueue;
  }

  public void setExecuteTasks(boolean executeTasks) {
    this.executeTasks = executeTasks;
  }
}
