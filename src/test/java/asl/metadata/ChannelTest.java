package asl.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChannelTest {

  @Test
  public final void testChannel_Valid_Standard() throws Exception {
    Channel channel = new Channel("00", "LH1");
    assertEquals(channel.getLocation(), "00");
    assertEquals(channel.getChannel(), "LH1");
  }

  @Test
  public final void testChannel_Valid_NullLocation() throws Exception {
    Channel channel = new Channel(null, "BH1");
    assertEquals(channel.getLocation(), "--");
    assertEquals(channel.getChannel(), "BH1");
  }

  @Test
  public final void testChannel_InvalidChannelLength() throws Exception {
    Channel channel = new Channel("00", "LH");
    assertEquals(channel.getLocation(), "00");
    assertEquals(channel.getChannel(), null);
  }

  @Test(expected = ChannelException.class)
  public final void testSetChannel_InvalidChannelLength() throws Exception {
    Channel channel = new Channel("00", "LH1");
    assertEquals(channel.getLocation(), "00");
    assertEquals(channel.getChannel(), "LH1");

    channel.setChannel("LH");
  }

  @Test
  public final void testSetChannel_Valid() throws Exception {
    Channel channel = new Channel("00", "LH1");
    assertEquals(channel.getLocation(), "00");
    assertEquals(channel.getChannel(), "LH1");

    channel.setChannel("BN1");
    assertEquals(channel.getLocation(), "00");
    assertEquals(channel.getChannel(), "BN1");
  }

  @Test
  public final void testValidLocationCodeValidDashes() throws Exception {
    assertTrue(Channel.validLocationCode("--"));
  }

  @Test
  public final void testValidLocationCode_ValidNumerical() throws Exception {
    assertTrue(Channel.validLocationCode("10"));
    assertTrue(Channel.validLocationCode("99"));
    assertTrue(Channel.validLocationCode("00"));
    assertTrue(Channel.validLocationCode("01"));
  }

  @Test
  public final void testValidLocationCode_InvalidForm_ValidLength() throws Exception {
    assertFalse(Channel.validLocationCode("1A"));
    assertFalse(Channel.validLocationCode("AA"));
    assertFalse(Channel.validLocationCode("  "));
    assertFalse(Channel.validLocationCode("1-"));
    assertFalse(Channel.validLocationCode("**"));
  }

  @Test
  public final void testValidLocationCode_InvalidShortLength() throws Exception {
    assertFalse(Channel.validLocationCode("1"));
    assertFalse(Channel.validLocationCode("A"));
    assertFalse(Channel.validLocationCode(" "));
    assertFalse(Channel.validLocationCode("0"));
    assertFalse(Channel.validLocationCode("*"));
    assertFalse(Channel.validLocationCode("-"));
    assertFalse(Channel.validLocationCode(""));
  }

  @Test
  public final void testValidLocationCode_InvalidLongLength() throws Exception {
    assertFalse(Channel.validLocationCode("111"));
    assertFalse(Channel.validLocationCode("AAA"));
    assertFalse(Channel.validLocationCode("   "));
    assertFalse(Channel.validLocationCode("100"));
    assertFalse(Channel.validLocationCode("***"));
    assertFalse(Channel.validLocationCode("---"));
    assertFalse(Channel.validLocationCode("90*"));
  }

  @Test
  public final void testValidBandCode_InvalidLength() throws Exception {
    assertFalse(Channel.validBandCode("11"));
    assertFalse(Channel.validBandCode("AA"));
    assertFalse(Channel.validBandCode(""));
  }

  @Test
  public final void testValidBandCode_ValidCodes() throws Exception {
    String[] validBands = {"F", "G", "D", "C", "E", "S", "H", "B", "M", "L", "V", "U", "R", "P",
        "T", "Q", "A", "O"};
    for (String band : validBands) {
      assertTrue(Channel.validBandCode(band));
    }
  }

  @Test
  public final void testValidBandCode_InvalidUpperCaseCodes() throws Exception {
    String[] invalidBands = {"I", "J", "K", "N", "W", "X", "Y", "Z"};
    for (String band : invalidBands) {
      assertFalse(Channel.validBandCode(band));
    }
  }

  @Test
  public final void testValidBandCode_InvalidMiscCodes() throws Exception {
    String[] invalidBands = {"1", "f", "e", "l", " ", "*", "-", "#"};
    for (String band : invalidBands) {
      assertFalse(Channel.validBandCode(band));
    }
  }

  @Test
  public final void testValidInstrumentCode_ValidCodes() throws Exception {
    String[] invalidInstruments = {"H", "L", "G", "M", "N", "D", "F", "I", "K", "R", "W", "C", "E"};
    for (String code : invalidInstruments) {
      assertTrue(Channel.validInstrumentCode(code));
    }
  }

  @Test
  public final void testValidInstrumentCode_InvalidLength() throws Exception {
    assertFalse(Channel.validInstrumentCode("11"));
    assertFalse(Channel.validInstrumentCode("HH"));
    assertFalse(Channel.validInstrumentCode(""));
  }

  @Test
  public final void testValidInstrumentCode_InvalidCodes() throws Exception {
    //May change in the future if we permit more instrument types.
    String[] invalidInstruments = {"A", "B", "J", "O", "P", "Q", "S", "T", "U", "V", "X", "Y", "Z"};
    for (String code : invalidInstruments) {
      assertFalse(Channel.validInstrumentCode(code));
    }
  }

  @Test
  public final void testValidInstrumentCode_InvalidMiscCodes() throws Exception {
    String[] invalidInstruments = {"1", "f", "e", "l", " ", "*", "-", "#"};
    for (String code : invalidInstruments) {
      assertFalse(Channel.validBandCode(code));
    }
  }

  @Test
  public final void testIsContinousChannel_MarkedContinous() throws Exception {
    assertTrue(Channel.isContinousChannel("CG"));
    assertTrue(Channel.isContinousChannel("CH"));
    assertTrue(Channel.isContinousChannel("CW"));
    assertTrue(Channel.isContinousChannel("C"));
  }

  @Test
  public final void testIsContinousChannel_MarkedContinousBadly() throws Exception {
    assertFalse(Channel.isContinousChannel("GC"));
    assertFalse(Channel.isContinousChannel("HC"));
    assertFalse(Channel.isContinousChannel("WC"));
  }

  @Test
  public final void testIsContinousChannel_GuessContinous() throws Exception {
    assertTrue(Channel.isContinousChannel("G"));
    assertTrue(Channel.isContinousChannel("H"));
  }

  @Test
  public final void testIsContinousChannel_NotContinous() throws Exception {
    assertFalse(Channel.isContinousChannel("TG"));
    assertFalse(Channel.isContinousChannel("TC"));
    assertFalse(Channel.isContinousChannel("TW"));
    assertFalse(Channel.isContinousChannel("T"));
  }

  @Test
  public final void testIsDerivedChannel_Derived() throws Exception {
    Channel channel = new Channel("00", "LHED");
    assertTrue(channel.isDerivedChannel());

    channel = new Channel("00-10", "LHED-LHED");
    assertTrue(channel.isDerivedChannel());
  }

  @Test
  public final void testIsDerivedChannel_NotDerived() throws Exception {
    Channel channel = new Channel("00", "LHE");
    assertFalse(channel.isDerivedChannel());

    channel = new Channel("10", "LH1");
    assertFalse(channel.isDerivedChannel());
  }

  @Test
  public final void testIsDerivedChannel_TooShort() throws Exception {
    Channel channel = new Channel("00", "LH");
    assertFalse(channel.isDerivedChannel());

    channel = new Channel("10", "LH");
    assertFalse(channel.isDerivedChannel());
  }

  @Test
  public final void testValidOrientationCode_ValidCodes() throws Exception {
    String[] invalidOrientations = {"1", "2", "3", "N", "E", "Z", "U", "V", "W"};
    for (String orientation : invalidOrientations) {
      assertTrue(Channel.validOrientationCode(orientation));
    }
  }

  @Test
  public final void testValidOrientationCode_InvalidCodes() throws Exception {
    String[] invalidOrientations = {"0", "4", "5", "A", "B", "C", "T", "R", "D"};
    for (String orientation : invalidOrientations) {
      assertFalse(Channel.validOrientationCode(orientation));
    }
  }

  @Test
  public final void testValidOrientationtCode_InvalidLength() throws Exception {
    assertFalse(Channel.validInstrumentCode("11"));
    assertFalse(Channel.validInstrumentCode("ND"));
    assertFalse(Channel.validInstrumentCode("ED"));
    assertFalse(Channel.validInstrumentCode(""));
  }

}
