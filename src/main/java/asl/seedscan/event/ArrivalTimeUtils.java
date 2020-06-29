package asl.seedscan.event;

import asl.metadata.meta_new.StationMeta;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrivalTimeUtils {

  private static final Logger logger = LoggerFactory.getLogger(ArrivalTimeUtils.class);

  public static long getPArrivalTime(EventCMT eventCMT, StationMeta stationMeta, String callingMetric)
      throws ArrivalTimeException {
    // we include the logger so we can figure out which metric is throwing up the error
    double eventLatitude = eventCMT.getLatitude();
    double eventLongitude = eventCMT.getLongitude();
    double eventDepth = eventCMT.getDepth();
    double stationLatitude = stationMeta.getLatitude();
    double stationLongitude = stationMeta.getLongitude();
    double greatCircleArc = SphericalCoords
        .distance(eventLatitude, eventLongitude, stationLatitude, stationLongitude);
    TauP_Time timeTool;
    try {
      timeTool = new TauP_Time("prem");
      timeTool.parsePhaseList("P");
      timeTool.setSourceDepth(eventDepth);
      timeTool.calculate(greatCircleArc);
    } catch (TauModelException e) {
      //Arrival times are not determinable.
      logger.error("{} -- called by metric {}", e.getMessage(), callingMetric);
      throw new ArrivalTimeException(e.getMessage());
    }

    List<Arrival> arrivals = timeTool.getArrivals();

    double arrivalTimeP;
    if (arrivals.size() > 0 && arrivals.get(0).getName().equals("P")) {
      arrivalTimeP = arrivals.get(0).getTime();
    } else {
      logger.info("Got an arrival, but it was not a P-wave, called by metric {}", callingMetric);
      throw new ArrivalTimeException("Arrival time found was not a P-wave");
    }

    logger.info(
        "Event:{} <eventLatitude,eventLongitude> = <{}, {}> Station:{} <{}, {}> greatCircleArc={} tP={}",
        eventCMT.getEventID(), eventLatitude, eventLongitude, stationMeta.getStation(),
        stationLatitude, stationLongitude, greatCircleArc, arrivalTimeP);

    return Math.round(arrivalTimeP * 1000); // get the arrival time in ms
  }

  public static class ArrivalTimeException extends Exception {
    private static final long serialVersionUID = 6851116640460104395L;

    ArrivalTimeException(String message) {
      super(message);
    }
  }
}
