package timeutils;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TimeseriesTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testDetrend() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testDemean() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testCostaper() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testInterpolate() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

	@Test
	public final void testRotate45() throws Exception {
		double az1 = 45;
		double az2 = 135;
		double[] x = {1,1,1,1,1,1,1};
		double[] y = {0,0,0,0,0,0,0};
		double[] n = {0,0,0,0,0,0,0};
		double[] e = {0,0,0,0,0,0,0};
		
		Double expected = (double)Math.round(0.70710678118 * 10000000d) / 10000000d; //sin(45 deg)
		
		
		Timeseries.rotate_xy_to_ne(az1, az2, x, y, n, e);
		for(int i = 0; i < n.length; i++){
			assertEquals(new Double((double)Math.round(n[i] * 10000000d) / 10000000d), expected);
			assertEquals(new Double((double)Math.round(e[i] * 10000000d) / 10000000d), expected);
		}
	}
	
	@Test
	public final void testRotate330() throws Exception {
		double az1 = 330;
		double az2 = 60;
		double[] x = {1,1,1,1,1,1,1};
		double[] y = {1,1,1,1,1,1,1};
		double[] n = {0,0,0,0,0,0,0};
		double[] e = {0,0,0,0,0,0,0};
		
		Double expectedN = (double)Math.round((0.5+0.86602540378) * 10000000d) / 10000000d; //sin(60) + cos(60)
		Double expectedE = (double)Math.round((-0.5+0.86602540378) * 10000000d) / 10000000d; //sin(330) + cos(330)
		
		Timeseries.rotate_xy_to_ne(az1, az2, x, y, n, e);
		for(int i = 0; i < n.length; i++){
			assertEquals(new Double((double)Math.round(n[i] * 10000000d) / 10000000d), expectedN);
			assertEquals(new Double((double)Math.round(e[i] * 10000000d) / 10000000d), expectedE);
		}
	}

}
