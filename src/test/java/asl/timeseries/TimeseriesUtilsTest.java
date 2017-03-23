package asl.timeseries;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeseriesUtilsTest {

  @Test
  public final void testDetrendNoSlope() throws Exception {
    double[] x = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    TimeseriesUtils.detrend(x);

    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double(x[i]), new Double(0.0));
    }

  }

  @Test
  public final void testDetrendCycle() throws Exception {
    double[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 19, 18, 17,
        16, 15, 14,
        13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
    Double[] answer = {-9d, -8d, -7d, -6d, -5d, -4d, -3d, -2d, -1d, 0d, 1d, 2d, 3d, 4d, 5d, 6d, 7d,
        8d, 9d, 10d,
        9d, 8d, 7d, 6d, 5d, 4d, 3d, 2d, 1d, 0d, -1d, -2d, -3d, -4d, -5d, -6d, -7d, -8d, -9d};

    TimeseriesUtils.detrend(x);
    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double(Math.round(x[i])), answer[i]);
    }
  }

  @Test
  public final void testDetrendLinear() throws Exception {
    double[] x = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20};

    TimeseriesUtils.detrend(x);
    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double(Math.round(x[i])), new Double(0));
    }
  }

  @Test
  public final void testDemean0s() throws Exception {
    double[] x = {1, 1, 1, 1, 1, 1, 1, 1, 1};
    TimeseriesUtils.demean(x);
    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double((double) Math.round(x[i] * 10000000d) / 10000000d), new Double(0.0));
    }
  }

  @Test
  public final void testDemean1to9() throws Exception {
    double[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    Double[] expected = {-4d, -3d, -2d, -1d, 0d, 1d, 2d, 3d, 4d};
    TimeseriesUtils.demean(x);
    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double((double) Math.round(x[i] * 10000000d) / 10000000d), expected[i]);
    }
  }

  @Test
  public final void testCostaperNoWidth() throws Exception {
    double[] x = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    double power = TimeseriesUtils.costaper(x, 0);

    assertEquals(new Double(Math.round(power)), new Double(16));
    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double(x[i]), new Double(1));
    }
  }

  @Test
  public final void testCostaperQuarter() throws Exception {
    double[] x = {5, 5, 5, 5, 5};
    Double[] answer = {0d, 4.5d, 5d, 4.5d, 0d};

    double power = TimeseriesUtils.costaper(x, 0.25);

    assertEquals(new Double(Math.round(power)), new Double(4));

    for (int i = 0; i < x.length; i++) {
      assertEquals(new Double((double) Math.round(x[i] * 10d) / 10d), answer[i]);
    }
  }

  @Test
  public final void testInterpolateBasic() throws Exception {
    double[] X = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    double[] Y = {1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 4, 3, 2, 1};
    double[] Z = {1, 2.5, 3, 3.5, 7, 10.1};

    Double[] answer = {1.0d, 2.5d, 3.0d, 3.5d, 7.0d, 7.9d};

    double[] result = TimeseriesUtils.interpolate(X, Y, Z);

    // Super basic interpolation. If it isn't within 1 tenth of the answer,
    // we should interpolate differently.
    for (int i = 0; i < result.length; i++) {
      assertEquals(new Double((double) Math.round(result[i] * 10d) / 10d), answer[i]);
    }
  }

  @Test
  public final void testRotate45() throws Exception {
    double az1 = 45;
    double az2 = 135;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {0, 0, 0, 0, 0, 0, 0};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};

    Double expected = (double) Math.round(0.70710678118 * 10000000d) / 10000000d; // sin(45
    // deg)

    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
    for (int i = 0; i < n.length; i++) {
      assertEquals(new Double((double) Math.round(n[i] * 10000000d) / 10000000d), expected);
      assertEquals(new Double((double) Math.round(e[i] * 10000000d) / 10000000d), expected);
    }
  }

  @Test
  public final void testRotate330() throws Exception {
    double az1 = 330;
    double az2 = 60;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {1, 1, 1, 1, 1, 1, 1};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};

    Double expectedN =
        (double) Math.round((0.5 + 0.86602540378) * 10000000d) / 10000000d; // sin(60)
    // +
    // cos(60)
    Double expectedE =
        (double) Math.round((-0.5 + 0.86602540378) * 10000000d) / 10000000d; // sin(330)
    // +
    // cos(330)

    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
    for (int i = 0; i < n.length; i++) {
      assertEquals(new Double((double) Math.round(n[i] * 10000000d) / 10000000d), expectedN);
      assertEquals(new Double((double) Math.round(e[i] * 10000000d) / 10000000d), expectedE);
    }
  }

  @Test(expected = TimeseriesException.class)
  public final void testRotateHighAzimuthXException() throws Exception {
    double az1 = 361;
    double az2 = 291;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {1, 1, 1, 1, 1, 1, 1};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};
    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
  }

  @Test(expected = TimeseriesException.class)
  public final void testRotateLowAzimuthXException() throws Exception {
    double az1 = 361;
    double az2 = 291;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {1, 1, 1, 1, 1, 1, 1};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};
    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
  }

  @Test(expected = TimeseriesException.class)
  public final void testRotateHighAzimuthYException() throws Exception {
    double az1 = 271;
    double az2 = 361;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {1, 1, 1, 1, 1, 1, 1};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};
    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
  }

  @Test(expected = TimeseriesException.class)
  public final void testRotateLowAzimuthYException() throws Exception {
    double az1 = 88;
    double az2 = -1;
    double[] x = {1, 1, 1, 1, 1, 1, 1};
    double[] y = {1, 1, 1, 1, 1, 1, 1};
    double[] n = {0, 0, 0, 0, 0, 0, 0};
    double[] e = {0, 0, 0, 0, 0, 0, 0};
    TimeseriesUtils.rotate_xy_to_ne(az1, az2, x, y, n, e);
  }

}
