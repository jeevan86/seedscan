package asl.seedscan.metrics;

import static asl.seedscan.metrics.PulseDetectionMetric.getCenteredMovingAverage;
import static asl.seedscan.metrics.PulseDetectionMetric.getCenteredMovingAveragePercentiles;
import static asl.seedscan.metrics.PulseDetectionMetric.getStepFunction;
import static asl.seedscan.metrics.PulseDetectionMetric.removeResponseOnTimeDomainData;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMetaException;
import asl.testutils.ResourceManager;
import asl.utils.FFTResult;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.Test;

public class PulseDetectionMetricTest {

  @Test
  public void testCrossCorrelation() {
    double[] x = new double[175];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    double[] y = new double[50];
    for (int i = 0; i < y.length; ++i) {
      y[i] = Math.pow(200 + i, 2);
    }

    double[] corr = PulseDetectionMetric.crossCorrelate(x, y)[0];
    double[] expect = new double[corr.length];
    Arrays.fill(expect, 1.);
    assertArrayEquals(expect, corr, 1E-5);
  }

  @Test
  public void testCrossCorrelationScale() {
    double[] x = new double[175];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    double[] y = new double[50];
    for (int i = 0; i < y.length; ++i) {
      y[i] = Math.pow(200 + i, 2);
    }

    double[] scale = PulseDetectionMetric.crossCorrelate(x, y)[1];
    double[] expect = new double[x.length - y.length];
    Arrays.fill(expect, 1.);
    assertArrayEquals(expect, scale, 1E-5);
  }

  @Test
  public void testDifferenceFunction() {
    double[] input = new double[]{-1.623948135223775e-08, -1.642077695229638e-08,
        -1.6599828272386355e-08, -1.677636735627786e-08, -1.6950740423119735e-08,
        -1.7123084318715335e-08, -1.729335078044176e-08, -1.7461555850362867e-08,
        -1.7627593725363494e-08, -1.779119201926566e-08, -1.7952397845231915e-08,
        -1.8111747516349007e-08, -1.826958827074384e-08, -1.8425556348968104e-08,
        -1.857907487807333e-08, -1.8730073432708538e-08, -1.8878932934895646e-08,
        -1.9025938213278464e-08, -1.9170980385417726e-08, -1.9313763521242517e-08};
    double[] output = PulseDetectionMetric.getPerPointDifference(input);
    double[] expected = new double[]{
        0.0, -1.8129560005862894e-10, -1.7905132008997637e-10, -1.765390838915038e-10,
        -1.7437306684187645e-10, -1.7234389559559933e-10, -1.7026646172642398e-10,
        -1.6820506992110842e-10, -1.6603787500062672e-10, -1.635982939021668e-10,
        -1.6120582596625492e-10, -1.5934967111709196e-10, -1.5784075439483228e-10,
        -1.5596807822426487e-10, -1.535185291052263e-10, -1.50998554635207e-10,
        -1.4885950218710864e-10, -1.470052783828172e-10, -1.4504217213926246e-10,
        -1.4278313582479068e-10
    };
    assertArrayEquals(expected, output, 1E-10);
  }

  @Test
  public void testMovingAverageResult() {
    double[] x = new double[25000];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    double[][] result = getCenteredMovingAveragePercentiles(x, 240);
    int[] lowestComparison = new int[]{
        487, 513, 513, 513, 540, 540, 540, 567, 567, 567, 595, 595, 595, 623, 623, 623, 652, 652,
        652, 682, 682, 682, 713, 713, 713, 744, 744, 744, 776, 776, 776, 808, 808, 808, 841, 841,
        841, 875, 875, 875, 910, 910, 910, 945, 945, 945, 981, 981, 981, 1017, 1017, 1017, 1054,
        1054, 1054, 1092, 1092, 1092, 1131, 1131, 1131, 1170, 1170, 1170, 1210, 1210, 1210, 1250,
        1250, 1250, 1291, 1291, 1291, 1333, 1333, 1333, 1376, 1376, 1376, 1419, 1419, 1419, 1463,
        1463, 1463, 1507, 1507, 1507, 1552, 1552, 1552, 1598, 1598, 1598, 1645, 1645, 1645, 1692,
        1692, 1692, 1740, 1740, 1740, 1788, 1788, 1788, 1837, 1837, 1837, 1887, 1887, 1887, 1938,
        1938, 1938, 1989, 1989, 1989, 2041, 2041, 2120, 2201, 2284, 2369, 2456, 2545, 2636, 2729,
        2824, 2921, 3020, 3121, 3224, 3329, 3436, 3545, 3656, 3769, 3884, 4001, 4120, 4241, 4364,
        4489, 4616, 4745, 4876, 5009, 5144, 5281, 5420, 5561, 5704, 5849, 5996, 6145, 6296, 6449,
        6604, 6761, 6920, 7081, 7244, 7409, 7576, 7745, 7916, 8089, 8264, 8441, 8620, 8801, 8984,
        9169, 9356, 9545, 9736, 9929, 10124, 10321, 10520, 10721, 10924, 11129, 11336, 11545, 11756,
        11969, 12184, 12401, 12620, 12841, 13064, 13289, 13516, 13745, 13976, 14209, 14444, 14681,
        14920, 15161, 15404, 15649, 15896, 16145, 16396, 16649, 16904, 17161, 17420, 17681, 17944
    };
    for (int i = 0; i < lowestComparison.length; ++i) {
      assertEquals(lowestComparison[i], (int) result[0][i]);
    }
  }

  @Test
  public void testCenteredMovingAverage() {
    double[] x = new double[250];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    double[] result = getCenteredMovingAverage(x, 240);
    int[] expect = new int[]{
        4661, 4740, 4820, 4900, 4981, 5063, 5146, 5229, 5313, 5397, 5482, 5568, 5655, 5742, 5830,
        5918, 6007, 6097, 6188, 6279, 6371, 6463, 6556, 6650, 6745, 6840, 6936, 7032, 7129, 7227,
        7326, 7425, 7525, 7625, 7726, 7828, 7931, 8034, 8138, 8242, 8347, 8453, 8560, 8667, 8775,
        8883, 8992, 9102, 9213, 9324, 9436, 9548, 9661, 9775, 9890, 10005, 10121, 10237, 10354,
        10472, 10591, 10710, 10830, 10950, 11071, 11193, 11316, 11439, 11563, 11687, 11812, 11938,
        12065, 12192, 12320, 12448, 12577, 12707, 12838, 12969, 13101, 13233, 13366, 13500, 13635,
        13770, 13906, 14042, 14179, 14317, 14456, 14595, 14735, 14875, 15016, 15158, 15301, 15444,
        15588, 15732, 15877, 16023, 16170, 16317, 16465, 16613, 16762, 16912, 17063, 17214, 17366,
        17518, 17671, 17825, 17980, 18135, 18291, 18447, 18604, 18762, 19000, 19240, 19482, 19726,
        19972, 20220, 20470, 20722, 20976, 21232, 21490, 21750, 21841, 21933, 22026, 22119, 22213,
        22307, 22402, 22498, 22595, 22692, 22790, 22888, 22987, 23087, 23188, 23289, 23391, 23493,
        23596, 23700, 23805, 23910, 24016, 24122, 24229, 24337, 24446, 24555, 24665, 24775, 24886,
        24998, 25111, 25224, 25338, 25452, 25567, 25683, 25800, 25917, 26035, 26153, 26272, 26392,
        26513, 26634, 26756, 26878, 27001, 27125, 27250, 27375, 27501, 27627, 27754, 27882, 28011,
        28140, 28270, 28400, 28531, 28663, 28796, 28929, 29063, 29197, 29332, 29468, 29605, 29742,
        29880, 30018, 30157, 30297, 30438, 30579, 30721, 30863, 31006, 31150, 31295, 31440, 31586,
        31732, 31879, 32027, 32176, 32325, 32475, 32625, 32776, 32928, 33081, 33234, 33388, 33542,
        33697, 33853, 34010, 34167, 34325, 34483, 34642, 34802, 34963, 35124, 35286, 35448, 35611,
        35775, 35940, 36105, 36271, 36437, 36604, 36772, 36941, 37110
    };
    for (int i = 0; i < expect.length; ++i) {
      assertEquals(expect[i], (int) result[i]);
    }
  }

  @Test
  public void validateStepFunction() {
    double[] step = getStepFunction(0.1);
    double[] expect = new double[]{
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.5, 0.75, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
    };
    assertArrayEquals(expect, step, 1E-10);
  }

  @Test
  public void testResponseRemoval() throws ChannelMetaException {
    LocalDate dataDate = LocalDate.ofYearDay(2020, 104);
    String doy = String.format("%03d", dataDate.getDayOfYear());
    String networkName = "IU";
    String stationName = "HRV";
    String metadataLocation = "/metadata/rdseed/" + networkName + "-" + stationName + "-ascii.txt";
    String seedDataLocation = "/seed_data/" + networkName + "_" + stationName + "/" +
        dataDate.getYear() + "/" + doy + "";
    Station station1 = new Station(networkName, stationName);
    Channel vh2 = new Channel("00", "VH2");
    MetricData data =
        ResourceManager.getMetricData(seedDataLocation, metadataLocation, dataDate, station1);
    double[] trace = data.getDetrendedPaddedDayData(vh2);
    FFTResult.cosineTaper(trace, 0.05); // taper done in-place
    double[] expectIn = new double[]{0.0, 0.014171871865102091, 0.05636303719035035,
        0.12810849831533347, 0.22686973075509226, 0.35277744608151057, 0.5022066639770922,
        0.6769645552597138, 0.8908051995546483, 1.116505990813392, 1.3424455622444849,
        1.6240153936506891};
    // just a quick check to ensure that the preprocessing is correct
    for (int i = 0; i < expectIn.length; ++i) {
      assertEquals(expectIn[i], trace[i], 1E-10);
    }
    ChannelMeta meta = data.getMetaData().getChannelMetadata(vh2);
    trace = removeResponseOnTimeDomainData(trace, meta, 0.001);

    double[] compareStartAgainst = new double[]{6.75200030732105E-8, 6.753205208943251E-8,
        6.754394343446055E-8, 6.755661883395824E-8, 6.756891660419617E-8, 6.758210271372274E-8,
        6.759478153187314E-8, 6.760877593573377E-8, 6.762256098771215E-8, 6.763603882312766E-8,
        6.765001845277449E-8, 6.766650078579518E-8, 6.768116724909628E-8, 6.769551941043154E-8,
        6.771147328422025E-8, 6.773092901050929E-8, 6.77489535773228E-8, 6.776520086919118E-8,
        6.77814287359702E-8, 6.780130470541931E-8};

    for (int i = 0; i < compareStartAgainst.length; ++i) {
      assertEquals("failed at index " + i, compareStartAgainst[i], trace[i], 1E-12);
    }
  }

}
