package asl.seedscan.scanner;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.scanner.scanworker.ScanWorker;

/**
 * Mock ScanManager for testing purposes
 */
public class ScanManagerMock extends ScanManager {

  public ScanManagerMock(MetricDatabase database,
      MetaGenerator metaGenerator) {
    super(database, metaGenerator);
  }

  @Override
  public void addTask(ScanWorker task) {

  }
}
