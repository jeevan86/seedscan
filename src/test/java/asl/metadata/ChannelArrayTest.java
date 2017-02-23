package asl.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ChannelArrayTest {


  @Test
  public void testChannelArray_Location_ThreeStringChannel() throws Exception {
    ChannelArray array = new ChannelArray("00", "LH1", "LH2", "LHZ");
    List<Channel> list = array.getChannels();
    assertTrue(list.contains(new Channel("00", "LH1")));
    assertTrue(list.contains(new Channel("00", "LH2")));
    assertTrue(list.contains(new Channel("00", "LHZ")));
    assertEquals(3, list.size());
  }

  @Test
  public void testChannelArray_Location_OneStringChannel() throws Exception {
    ChannelArray array = new ChannelArray("10", "LHZ");
    List<Channel> list = array.getChannels();
    assertTrue(list.contains(new Channel("10", "LHZ")));
    assertEquals(1, list.size());
  }

  @Test
  public void testChannelArray_TwoChannel() throws Exception {
    Channel a = new Channel("00", "LH1");
    Channel b = new Channel("10", "LH2");
    ChannelArray array = new ChannelArray(a, b);
    List<Channel> list = array.getChannels();
    assertTrue(list.contains(a));
    assertTrue(list.contains(b));
    assertEquals(2, list.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getChannels_Unmodifiable() throws Exception {
    ChannelArray array = new ChannelArray("00", "LH1", "LH2", "LHZ");
    List<Channel> list = array.getChannels();
    list.add(new Channel("10", "LH1"));
  }

}