package asl.seedscan.scanner.scanworker;

import asl.metadata.Station;
import asl.seedscan.database.DatabaseScan;
import asl.seedscan.scanner.ScanManager;
import asl.util.Logging;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This worker retrieves a Scan from the database. Determines if it is a
 * Station and splits it into station scans if it is not.
 *
 * @author jholland - USGS
 */
public class RetrieveScan extends ScanWorker {

  private static final Logger logger = LoggerFactory
      .getLogger(asl.seedscan.scanner.scanworker.RetrieveScan.class);

  public RetrieveScan(ScanManager manager) {
    super(manager);
  }

  @Override
  public void run() {
    //Runtime Exceptions thrown here are not caught anywhere else.
    Object lock = manager.database.getLockObject();
    try {
      synchronized (lock) {
        DatabaseScan newScan = manager.database.takeNextScan();

        if (newScan != null) {
          parseScan(newScan);
          // Add new Retriever to queue since we know more probably exist.
          manager.addTask(new RetrieveScan(manager));
        } else {
          logger.info("Database has no Scans left!");
        }
        /*
         * Don't bother adding a new Retrieving Task since DB is empty. A
         * different process will handle this.
         */
      }

    } catch (Exception e) {
      String message = Logging.prettyExceptionWithCause(e);
      logger.error(message);
      manager.database.insertError(message);
    }
  }

  void parseScan(DatabaseScan newScan) {
    long dayLength = ChronoUnit.DAYS.between(newScan.startDate, newScan.endDate);
    String[] networks = null;
    if (newScan.network != null) {
      networks = newScan.network.split(",");
    }
    String[] stations = null;
    if (newScan.station != null) {
      stations = newScan.station.split(",");
    }

    // Check if it is a Station Scan.
    if (networks != null && stations != null && stations.length == 1 && networks.length == 1
        && dayLength <= 30) {
      manager.addTask(new StationScan(manager, newScan));
    }
    // Split the non Station Scan into Station Scans
    else {
      List<Station> possibleStations = manager.metaGenerator.getStationList(networks, stations);
      LocalDate start = newScan.startDate;
      LocalDate end;
      do {
        end = start.plusDays(30);
        if (end.compareTo(newScan.endDate) > 0) {
          end = newScan.endDate;
        }
        for (Station station : possibleStations) {
          manager.database.insertChildScan(
              newScan.scanID,
              station.getNetwork(),
              station.getStation(),
              newScan.location,
              newScan.channel,
              newScan.metricName,
              start, end,
              newScan.priority,
              newScan.deleteExisting);
        }

        start = end.plusDays(1);
      } while (!end.equals(newScan.endDate));
    }
  }

  @Override
  public Integer getBasePriority() {
    // This should always run after everything else.
    return Integer.MAX_VALUE;
  }

  @Override
  public Long getFinePriority() {
    // This should always run after everything else.
    return Long.MAX_VALUE;
  }

}
