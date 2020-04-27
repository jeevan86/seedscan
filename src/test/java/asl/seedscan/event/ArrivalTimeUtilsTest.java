package asl.seedscan.event;

import static asl.seedscan.event.ArrivalTimeUtils.getPArrivalTime;
import static org.junit.Assert.assertEquals;

import asl.metadata.Blockette;
import asl.metadata.WrongBlocketteException;
import asl.metadata.meta_new.StationMeta;
import asl.seedscan.event.ArrivalTimeUtils.ArrivalTimeException;
import edu.sc.seis.TauP.SphericalCoords;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrivalTimeUtilsTest {

  private static final Logger logger = LoggerFactory
      .getLogger(ArrivalTimeUtilsTest.class);

  @Test
  public void checkPArrivalTimeCorrect() throws WrongBlocketteException, ArrivalTimeException {
    // construct a fake event and fake metadata and then get the arrival time from it
    LocalDateTime ldt = LocalDateTime.now();
    EventCMT eventCMT =
        new EventCMT.Builder("eventID").
            calendar(GregorianCalendar.from(ldt.atZone(ZoneOffset.UTC)))
            .depth(55).latitude(30).longitude(60).build();
    Blockette blockette = new Blockette(50);
    blockette.addFieldData(3, "Blockettename"); //name
    blockette.addFieldData(4, "70."); // latitude
    blockette.addFieldData(5, "25."); // longitude
    blockette.addFieldData(6, "0."); // elevation
    blockette.addFieldData(16, "IU"); // network
    StationMeta meta = new StationMeta(blockette, ldt);
    long pArrivalTime = getPArrivalTime(eventCMT, meta, "pArrivalTimeTester");
    assertEquals(486021L, pArrivalTime);
  }

}
