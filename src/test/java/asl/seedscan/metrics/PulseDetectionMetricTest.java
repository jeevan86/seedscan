package asl.seedscan.metrics;

import static asl.seedscan.metrics.PulseDetectionMetric.computeCrossCorrelationWithStep;
import static asl.seedscan.metrics.PulseDetectionMetric.envelopeConstraint;
import static asl.seedscan.metrics.PulseDetectionMetric.getCenteredMovingAverage;
import static asl.seedscan.metrics.PulseDetectionMetric.getCenteredMovingAveragePercentiles;
import static asl.seedscan.metrics.PulseDetectionMetric.getStepFunction;
import static asl.seedscan.metrics.PulseDetectionMetric.removeResponseOnTimeDomainData;
import static asl.seedscan.metrics.PulseDetectionMetric.sharpnessConstraint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import asl.metadata.Channel;
import asl.metadata.Station;
import asl.metadata.meta_new.ChannelMeta;
import asl.metadata.meta_new.ChannelMetaException;
import asl.testutils.ResourceManager;
import asl.utils.FFTResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
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
  public void testCrossCorrelationWithStep() {
    double[] x = new double[2500];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    // double[] stepFunction = PulseDetectionMetric.getStepFunction(0.1);
    double[] corr = PulseDetectionMetric.computeCrossCorrelationWithStep(x, 0.1)[0];
    double[] expect = {
        -0.026255574380320456, -0.02625557438032046, -0.02625557438032027, -0.026255574380320446,
        -0.02625557438032039, -0.02625557438032043, -0.026255574380320595, -0.026255574380320595,
        -0.02625557438032033, -0.02625557438032017, -0.02625557438032039, -0.026255574380320296,
        -0.02625557438032051, -0.026255574380320185, -0.02625557438032024, -0.026255574380320612,
        -0.02625557438032044, -0.026255574380320616, -0.026255574380320158, -0.026255574380320588,
        -0.026255574380320494, -0.026255574380320588, -0.026255574380320345, -0.026255574380320588,
        -0.02625557438032056, -0.026255574380320425, -0.02625557438032029, -0.026255574380320272,
        -0.02625557438032041, -0.026255574380320237, -0.026255574380320314, -0.026255574380320255,
        -0.02625557438032054, -0.026255574380320376, -0.026255574380320355, -0.026255574380320463,
        -0.02625557438032018, -0.026255574380320123, -0.026255574380320546, -0.026255574380320522,
        -0.0262555743803203, -0.026255574380320102, -0.026255574380319918, -0.026255574380320317,
        -0.02625557438031981, -0.026255574380319946, -0.026255574380320345, -0.026255574380320144,
        -0.026255574380319995, -0.026255574380320244, -0.026255574380320244, -0.026255574380320106,
        -0.026255574380320074, -0.026255574380320303, -0.026255574380320605, -0.0262555743803203,
        -0.026255574380320387, -0.026255574380320338, -0.026255574380320387, -0.026255574380320373,
        -0.026255574380320376, -0.02625557438032031, -0.026255574380320074, -0.02625557438032071,
        -0.02625557438032077, -0.026255574380320158, -0.026255574380320446, -0.02625557438032024,
        -0.02625557438032048, -0.02625557438032042, -0.02625557438032023, -0.026255574380320404,
        -0.026255574380320067, -0.02625557438032053, -0.026255574380320508, -0.026255574380320744,
        -0.026255574380320855, -0.026255574380320213, -0.026255574380320508, -0.02625557438032067,
        -0.026255574380319675, -0.026255574380320924, -0.02625557438032059, -0.026255574380320244,
        -0.02625557438032045, -0.026255574380320203, -0.026255574380320203, -0.02625557438032023,
        -0.026255574380320456, -0.026255574380320296, -0.026255574380320203, -0.026255574380320345,
        -0.026255574380320126, -0.026255574380320234, -0.026255574380320945, -0.026255574380320126,
        -0.026255574380320647, -0.026255574380320078, -0.026255574380320078, -0.026255574380319703
    };
    // double[] compExp = Arrays.copyOfRange(expect, 0, 100);
    double[] compCorr = Arrays.copyOfRange(corr, 0, 100);
    assertArrayEquals(expect, compCorr, 1E-10);
    // this is starting at point 1000 of the correlation
    expect = new double[]{
        -0.026255574380308355, -0.02625557438030713, -0.026255574380306144, -0.02625557438030054,
        -0.02625557438033636, -0.026255574380299886, -0.026255574380336974, -0.0262555743803203,
        -0.0262555743803203, -0.0262555743803203, -0.02625557438037925, -0.026255574380282986,
        -0.0262555743803203, -0.026255574380336523, -0.02625557438033579, -0.026255574380285404,
        -0.026255574380334188, -0.026255574380307907, -0.0262555743803203, -0.026255574380348628,
        -0.026255574380325594, -0.026255574380292062, -0.0262555743803203, -0.026255574380318027,
        -0.02625557438028781, -0.026255574380322094, -0.02625557438032038, -0.026255574380345408,
        -0.02625557438028812, -0.02625557438032038, -0.026255574380299886, -0.02625557438024318,
        -0.02625557438028375, -0.026255574380345408, -0.02625557438032038, -0.0262555743803203,
        -0.02625557438032038, -0.02625557438028497, -0.026255574380335125, -0.026255574380299886,
        -0.0262555743803203, -0.026255574380345315, -0.026255574380263144, -0.026255574380393634,
        -0.02625557438032038, -0.026255574380231666, -0.026255574380341637, -0.026255574380285127,
        -0.026255574380344773, -0.026255574380340846
    };
    compCorr = Arrays.copyOfRange(corr, 1000, 1050);
    assertArrayEquals(expect, compCorr, 1E-10);
  }

  @Test
  public void testCrossCorrelationWithStepScale() {
    double[] x = new double[2500];
    for (int i = 0; i < x.length; ++i) {
      x[i] = Math.pow(i, 2);
    }
    // double[] stepFunction = PulseDetectionMetric.getStepFunction(0.1);
    double[] scale = PulseDetectionMetric.computeCrossCorrelationWithStep(x, 0.1)[1];
    double[] expect = {
        -67.09650315958298, -67.09650315958287, -67.09650315958278, -67.09650315958275,
        -67.09650315958255, -67.09650315958275, -67.09650315958267, -67.09650315958267,
        -67.09650315958297, -67.09650315958287, -67.0965031595831, -67.09650315958287,
        -67.09650315958302, -67.09650315958223, -67.09650315958268, -67.09650315958322,
        -67.09650315958334, -67.09650315958318, -67.09650315958272, -67.09650315958298,
        -67.09650315958355, -67.09650315958302, -67.09650315958277, -67.09650315958298,
        -67.09650315958287, -67.09650315958248, -67.09650315958336, -67.09650315958328,
        -67.09650315958258, -67.09650315958291, -67.09650315958294, -67.09650315958278,
        -67.09650315958287, -67.09650315958287, -67.09650315958253, -67.09650315958318,
        -67.09650315958216, -67.09650315958206, -67.09650315958355, -67.09650315958314,
        -67.09650315958308, -67.09650315958294, -67.0965031595822, -67.09650315958332,
        -67.09650315958199, -67.0965031595824, -67.09650315958287, -67.09650315958227,
        -67.09650315958271, -67.09650315958298, -67.09650315958298, -67.09650315958224,
        -67.0965031595824, -67.09650315958298, -67.09650315958353, -67.09650315958318,
        -67.09650315958318, -67.09650315958318, -67.09650315958318, -67.09650315958312,
        -67.09650315958322, -67.09650315958332, -67.09650315958287, -67.09650315958389,
        -67.09650315958376, -67.09650315958292, -67.09650315958335, -67.09650315958308,
        -67.09650315958382, -67.09650315958332, -67.09650315958281, -67.09650315958262,
        -67.09650315958251, -67.0965031595835, -67.09650315958393, -67.0965031595836,
        -67.09650315958403, -67.09650315958264, -67.09650315958335, -67.09650315958379,
        -67.09650315958092, -67.09650315958433, -67.09650315958393, -67.09650315958264,
        -67.09650315958362, -67.09650315958291, -67.09650315958291, -67.09650315958214,
        -67.09650315958318, -67.09650315958254, -67.09650315958291, -67.09650315958298,
        -67.09650315958251, -67.09650315958291, -67.09650315958436, -67.09650315958251,
        -67.0965031595834, -67.09650315958257, -67.09650315958196, -67.09650315958082
    };
    // double[] compExp = Arrays.copyOfRange(expect, 0, 100);
    double[] scaleTrim = Arrays.copyOfRange(scale, 0, 100);
    assertArrayEquals(expect, scaleTrim, 1E-10);
  }

  @Test
  public void testCrossCorrelationStep_polyCurve() {
    int lengthOfParabola = 150;
    double[] x = new double[lengthOfParabola + 1];
    for (int i = 0; i < x.length; ++i) {
      x[i] = (-1 * Math.pow(i, 2)) + (lengthOfParabola * i);
    }
    assertEquals(0, x[0], 0);
    // assertEquals(5625, x[75], 0);
    assertEquals(0, x[lengthOfParabola], 0);
    double[] corr = PulseDetectionMetric.computeCrossCorrelationWithStep(x, 0.1)[0];
    double[] expect = {
        0.026255574380320508, 0.026255574380320452, 0.02625557438032045, 0.026255574380320328,
        0.026255574380320452, 0.026255574380320258, 0.026255574380320546, 0.026255574380320383,
        0.026255574380320452, 0.026255574380320532, 0.026255574380320355, 0.026255574380320564,
        0.02625557438032049, 0.02625557438032049, 0.026255574380320473, 0.02625557438032039,
        0.026255574380320224, 0.02625557438032039, 0.026255574380320546, 0.026255574380320678,
        0.02625557438032025, 0.026255574380320428, 0.026255574380320296, 0.02625557438032039,
        0.026255574380320324, 0.02625557438032065, 0.026255574380320428, 0.026255574380320487,
        0.026255574380320418, 0.02625557438032046, 0.02625557438032046, 0.026255574380320112,
        0.02625557438032028, 0.02625557438032054, 0.026255574380320692, 0.026255574380320428,
        0.026255574380320293, 0.026255574380320355, 0.026255574380320532, 0.0262555743803205,
        0.026255574380320494, 0.02625557438032057, 0.026255574380320577, 0.026255574380320196,
        0.026255574380320196, 0.02625557438032035, 0.026255574380320442, 0.026255574380320456,
        0.0262555743803204, 0.026255574380320498, 0.026255574380320265, 0.02625557438032022,
        0.02625557438032037, 0.026255574380320515, 0.026255574380320376, 0.026255574380320473,
        0.02625557438032043, 0.026255574380320543, 0.02625557438032032, 0.02625557438032039
    };
    assertArrayEquals(expect, Arrays.copyOfRange(corr, 0, corr.length - 1), 1E-10);
  }

  @Test
  public void testCrossCorrelationLongerStepFunction() {
    double sampleRate = 0.1;
    double[] x = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0625,
        0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
        0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
    double[] corr = computeCrossCorrelationWithStep(x, sampleRate)[0];
    double[] expect = {
        -0.20229529216511902, -0.22263120544672552, -0.24060667256744087, -0.2567064506592455,
        -0.27124200883463045, -0.2861858890455496, -0.29995260350450265, -0.3121238663240668,
        -0.32258057713403593, -0.3312890149103017, -0.3382356714666903, -0.3434050499343808,
        -0.3467718060914184, -0.3482981027114649, -0.34793293239001516, -0.3456121755896029,
        -0.34125891285907534, -0.334783811145941, -0.32608553116106387, -0.31505115917496196,
        -0.3015566934076419, -0.28546762933134323, -0.2666396969459989, -0.24491980936069,
        -0.22014728677895093, -0.19215542315769835, -0.16077346366643203, -0.1258290584324094,
        -0.0871512503774298, -0.04457404043942179, 0.0020594497540638817, 0.05289223134054563,
        0.10804817660253059, 0.16762633793454956, 0.23169503997690813, 0.3002860005951045,
        0.3733888090710991, 0.450946152759644, 0.5328502249120532, 0.6189407538485123,
        0.7090050559427601, 0.7961796642934212, 0.8734589726576713, 0.9337807325980657,
        0.970120346181026, 0.982237686573247, 0.9700873566315809, 0.9338202080383675,
        0.8737752719997147, 0.7970319274790154, 0.7106627958259245, 0.6216429811597434,
        0.5367712829985108, 0.4562245366800179, 0.3801263314712985, 0.3085486618327052,
        0.24151528356828117, 0.17900633281152306, 0.12096376822709888, 0.06729723489435704,
        0.017890009467799253, -0.027395241504520607, -0.06871108450450682, -0.10622044939772746,
        -0.14009233488361808, -0.17049810971888613, -0.19760839503271607, -0.22159048828864902,
        -0.24260627362804343, -0.2608105545881438, -0.2763497417170906, -0.2893608278336295,
        -0.2999705863688864, -0.3082949325928448, -0.3144383933912676, -0.3184936394273385,
        -0.3205410466745833, -0.3206482791122437, -0.3188699366905757, -0.31524743086375046,
        -0.3098095310071792, -0.3025747296816677, -0.2935584638431975, -0.282793768827065,
        -0.27039232114880807, -0.2567455786541392, -0.2433437304841074, -0.22839953974408403,
        -0.21161532618821027, -0.19252715821464367
    };
    assertArrayEquals(expect, corr, 1E-10);
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
  public void testPercentileMovingAverageResult() {
    double[] x = new double[25000];
    Arrays.setAll(x, i -> Math.pow(i, 2));
    double[][] result = getCenteredMovingAveragePercentiles(x, 240);
    double[] lowestComparison = new double[]{
        487.6666666666667, 513.5, 513.5, 513.5, 540.0, 540.0, 540.0, 567.1666666666666,
        567.1666666666666, 567.1666666666666, 595.0, 595.0, 595.0, 623.5, 623.5, 623.5,
        652.6666666666666, 652.6666666666666, 652.6666666666666, 682.5, 682.5, 682.5, 713.0, 713.0,
        713.0, 744.1666666666666, 744.1666666666666, 744.1666666666666, 776.0, 776.0, 776.0, 808.5,
        808.5, 808.5, 841.6666666666666, 841.6666666666666, 841.6666666666666, 875.5, 875.5, 875.5,
        910.0, 910.0, 910.0, 945.1666666666666, 945.1666666666666, 945.1666666666666, 981.0, 981.0,
        981.0, 1017.5, 1017.5, 1017.5, 1054.6666666666667, 1054.6666666666667, 1054.6666666666667,
        1092.5, 1092.5, 1092.5, 1131.0, 1131.0, 1131.0, 1170.1666666666667, 1170.1666666666667,
        1170.1666666666667, 1210.0, 1210.0, 1210.0, 1250.5, 1250.5, 1250.5, 1291.6666666666667,
        1291.6666666666667, 1291.6666666666667, 1333.5, 1333.5, 1333.5, 1376.0, 1376.0, 1376.0,
        1419.1666666666667, 1419.1666666666667, 1419.1666666666667, 1463.0, 1463.0, 1463.0, 1507.5,
        1507.5, 1507.5, 1552.6666666666667, 1552.6666666666667, 1552.6666666666667, 1598.5, 1598.5,
        1598.5, 1645.0, 1645.0, 1645.0, 1692.1666666666667, 1692.1666666666667, 1692.1666666666667
    };
    double[] middlestComparison = new double[]{
        3555.5, 3673.5, 3673.5, 3740.0, 3861.0, 3861.0, 3929.1666666666665, 4053.1666666666665,
        4053.1666666666665, 4123.0, 4250.0, 4250.0, 4321.5, 4451.5, 4451.5, 4524.666666666667,
        4657.666666666667, 4657.666666666667, 4732.5, 4868.5, 4868.5, 4945.0, 5084.0, 5084.0,
        5162.166666666667, 5304.166666666667, 5304.166666666667, 5384.0, 5529.0, 5529.0, 5610.5,
        5758.5, 5758.5, 5841.666666666667, 5992.666666666667, 5992.666666666667, 6077.5, 6231.5,
        6231.5, 6318.0, 6475.0, 6475.0, 6563.166666666667, 6723.166666666667, 6723.166666666667,
        6813.0, 6976.0, 6976.0, 7067.5, 7233.5, 7233.5, 7326.666666666667, 7495.666666666667,
        7495.666666666667, 7590.5, 7762.5, 7762.5, 7859.0, 8034.0, 8034.0, 8132.166666666667,
        8310.166666666666, 8310.166666666666, 8410.0, 8591.0, 8591.0, 8692.5, 8876.5, 8876.5,
        8979.666666666666, 9166.666666666666, 9166.666666666666, 9271.5, 9461.5, 9461.5, 9568.0,
        9761.0, 9761.0, 9869.166666666666, 10065.166666666666, 10065.166666666666, 10175.0, 10374.0,
        10374.0, 10485.5, 10687.5, 10687.5, 10800.666666666666, 11005.666666666666,
        11005.666666666666, 11120.5, 11328.5, 11328.5, 11445.0, 11656.0, 11656.0,
        11774.166666666666, 11988.166666666666, 11988.166666666666, 12108.0
    };
    for (int i = 0; i < lowestComparison.length; ++i) {
      assertEquals(lowestComparison[i], result[0][i], 1E-10);
    }
    for (int i = 0; i < middlestComparison.length; ++i) {
      assertEquals(middlestComparison[i], result[1][i], 1E-10);
    }
  }

  @Test
  public void testCenteredMovingAverage() {
    double[] x = new double[250];
    Arrays.setAll(x, i -> Math.pow(i, 2));
    double[] result = getCenteredMovingAverage(x, 240);
    double[] expect = new double[]{
        4661.0, 4740.166666666667, 4820.0, 4900.5, 4981.666666666667, 5063.5, 5146.0,
        5229.166666666667, 5313.0, 5397.5, 5482.666666666667, 5568.5, 5655.0, 5742.166666666667,
        5830.0, 5918.5, 6007.666666666667, 6097.5, 6188.0, 6279.166666666667, 6371.0, 6463.5,
        6556.666666666667, 6650.5, 6745.0, 6840.166666666667, 6936.0, 7032.5, 7129.666666666667,
        7227.5, 7326.0, 7425.166666666667, 7525.0, 7625.5, 7726.666666666667, 7828.5, 7931.0,
        8034.166666666667, 8138.0, 8242.5, 8347.666666666666, 8453.5, 8560.0, 8667.166666666666,
        8775.0, 8883.5, 8992.666666666666, 9102.5, 9213.0, 9324.166666666666, 9436.0, 9548.5,
        9661.666666666666, 9775.5, 9890.0, 10005.166666666666, 10121.0, 10237.5, 10354.666666666666,
        10472.5, 10591.0, 10710.166666666666, 10830.0, 10950.5, 11071.666666666666, 11193.5,
        11316.0, 11439.166666666666, 11563.0, 11687.5, 11812.666666666666, 11938.5, 12065.0,
        12192.166666666666, 12320.0, 12448.5, 12577.666666666666, 12707.5, 12838.0,
        12969.166666666666, 13101.0, 13233.5, 13366.666666666666, 13500.5, 13635.0,
        13770.166666666666, 13906.0, 14042.5, 14179.666666666666, 14317.5, 14456.0,
        14595.166666666666, 14735.0, 14875.5, 15016.666666666666, 15158.5, 15301.0,
        15444.166666666666, 15588.0, 15732.5, 15877.666666666666, 16023.5, 16170.0,
        16317.166666666666, 16465.0, 16613.5, 16762.666666666668, 16912.5, 17063.0,
        17214.166666666668, 17366.0, 17518.5, 17671.666666666668, 17825.5, 17980.0,
        18135.166666666668, 18291.0, 18447.5, 18604.666666666668, 18762.5, 19000.5, 19240.5,
        19482.5, 19726.5, 19972.5, 20220.5, 20470.5, 20722.5, 20976.5, 21232.5, 21490.5, 21750.5,
        21841.666666666668, 21933.5, 22026.0, 22119.166666666668, 22213.0, 22307.5,
        22402.666666666668, 22498.5, 22595.0, 22692.166666666668, 22790.0, 22888.5,
        22987.666666666668, 23087.5, 23188.0, 23289.166666666668, 23391.0, 23493.5,
        23596.666666666668, 23700.5, 23805.0, 23910.166666666668, 24016.0, 24122.5,
        24229.666666666668, 24337.5, 24446.0, 24555.166666666668, 24665.0, 24775.5,
        24886.666666666668, 24998.5, 25111.0, 25224.166666666668, 25338.0, 25452.5,
        25567.666666666668, 25683.5, 25800.0, 25917.166666666668, 26035.0, 26153.5,
        26272.666666666668, 26392.5, 26513.0, 26634.166666666668, 26756.0, 26878.5,
        27001.666666666668, 27125.5, 27250.0, 27375.166666666668, 27501.0, 27627.5,
        27754.666666666668, 27882.5, 28011.0, 28140.166666666668, 28270.0, 28400.5,
        28531.666666666668, 28663.5, 28796.0, 28929.166666666668, 29063.0, 29197.5,
        29332.666666666668, 29468.5, 29605.0, 29742.166666666668, 29880.0, 30018.5,
        30157.666666666668, 30297.5, 30438.0, 30579.166666666668, 30721.0, 30863.5,
        31006.666666666668, 31150.5, 31295.0, 31440.166666666668, 31586.0, 31732.5,
        31879.666666666668, 32027.5, 32176.0, 32325.166666666668, 32475.0, 32625.5,
        32776.666666666664, 32928.5, 33081.0, 33234.166666666664, 33388.0, 33542.5,
        33697.666666666664, 33853.5, 34010.0, 34167.166666666664, 34325.0, 34483.5,
        34642.666666666664, 34802.5, 34963.0, 35124.166666666664, 35286.0, 35448.5,
        35611.666666666664, 35775.5, 35940.0, 36105.166666666664, 36271.0, 36437.5,
        36604.666666666664, 36772.5, 36941.0, 37110.166666666664
    };
    for (int i = 0; i < expect.length; ++i) {
      assertEquals(expect[i], result[i], 1E-10);
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

  @Test
  public void testSharpnessConstraint() throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(new File(
        "src/test/resources/tests/sharpnessConstraintSample.csv"
    )));
    String[] unparsedNumbers = br.readLine().split(",");
    double[] points = new double[unparsedNumbers.length];
    Arrays.setAll(points, i -> Double.parseDouble(unparsedNumbers[i]));
    assertEquals(8640, points.length);
    Set<Integer> removed = sharpnessConstraint(points, 0.1, null);
    assertEquals(13, points.length - removed.size());
    int[] validIndices = new int[]{
        1963, 1964, 1965, 1966, 1967, 1968, 2167, 2168, 2169, 2229, 2230, 2231, 2232
    };
    for (int validIndex : validIndices) {
      assertFalse(removed.contains(validIndex));
    }
  }

  @Test
  public void testEnvelopeConstraint() throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(new File(
        "src/test/resources/tests/sharpnessConstraintSample.csv"
    )));
    String[] unparsedNumbers = br.readLine().split(",");
    double[] points = new double[unparsedNumbers.length];
    Arrays.setAll(points, i -> Double.parseDouble(unparsedNumbers[i]));
    assertEquals(8640, points.length);

    Set<Integer> retained = envelopeConstraint(points, 0.1);
    assertEquals(7287, retained.size());
    int[] validIndices = new int[]{
        1963, 1964, 1965, 1966, 1967, 1968, 2167, 2168, 2169, 2229, 2230, 2231, 2232
    };
    boolean passes = true;
    StringBuilder sb = new StringBuilder();
    for (int validIndex : validIndices) {
      if (!retained.contains(validIndex)) {
        passes = false;
        sb.append(validIndex).append(" was not retained\n");
      }
    }
    assertTrue(sb.toString(), passes);
  }

}