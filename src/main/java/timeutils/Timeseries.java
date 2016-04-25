package timeutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timeseries {
	private static final Logger logger = LoggerFactory.getLogger(timeutils.Timeseries.class);

	/**
	 * Performs an in place detrend on an array of timeseries data.
	 * 
	 * @param timeseries
	 *            array to detrend.
	 */
	public static void detrend(double[] timeseries) {
		int ndata = timeseries.length;
		double sumx = 0.0;
		double sumxx = 0.0;
		double sumy = 0.0;
		double sumxy = 0.0;

		for (int i = 0; i < ndata; i++) {
			sumx += (double) i;
			sumxx += (double) i * (double) i;
			sumy += timeseries[i];
			sumxy += (double) i * timeseries[i];
		}

		double del = sumxx - sumx * sumx / (double) ndata;
		double slope = sumxy - sumx * sumy / (double) ndata;
		slope /= del;
		double yoff = (sumxx * sumy - sumx * sumxy);
		yoff /= (double) ndata * del;

		for (int i = 0; i < ndata; i++) {
			timeseries[i] -= (slope * (double) i + yoff);
		}
	}

	/**
	 * Performs an in place demean on an array of timeseries data.
	 * 
	 * @param timeseries
	 *            demeaned in place.
	 * @throws RuntimeException
	 *             if timeseries.length == 0
	 */
	public static void demean(double[] timeseries) throws RuntimeException {
		double mean = 0;
		for (int i = 0; i < timeseries.length; i++) {
			mean += timeseries[i];
		}
		if (timeseries.length == 0) {
			throw new RuntimeException("debias: timeseries.length=0 --> No data!");
		} else {
			mean /= (double) timeseries.length;
			for (int i = 0; i < timeseries.length; i++) {
				timeseries[i] -= mean;
			}
		}
	}

	/**
	 * Performs an in place cosine taper on passed array of data.
	 * 
	 * @param timeseries
	 * @param width
	 * @return double related to power loss from taper.
	 */
	public static double costaper(double[] timeseries, double width) {
		int n = timeseries.length;
		double ramp = width * (double) n;
		double taper;
		double Wss = 0;

		for (int i = 0; i < ramp; i++) {
			taper = 0.5 * (1.0 - Math.cos((double) i * Math.PI / ramp));
			timeseries[i] *= taper;
			timeseries[n - i - 1] *= taper;
			Wss += 2.0 * taper * taper;
		}
		Wss += (n - 2. * ramp);
		return Wss;
	}

	/**
	 * Numerical Recipes cubic spline interpolation (spline.c and splint.c)
	 * Expects arrays with +1 offset: x[1,...,n], etc. - we will pass it arrays
	 * with 0 offset: x[0,1,...,n] and ignore the first points.
	 * 
	 * @param x
	 * @param y
	 * @param n
	 * @param yp1
	 * @param ypn
	 * @param y2
	 */
	private static void spline(double[] x, double[] y, int n, double yp1, double ypn, double[] y2) {

		double p, qn, sig, un;
		p = qn = sig = un = 0;

		// u=vector(1,n-1);
		double[] u = new double[n];

		if (yp1 > 0.99e30)
			y2[1] = u[1] = 0.0;
		else {
			y2[1] = -0.5;
			u[1] = (3.0 / (x[2] - x[1])) * ((y[2] - y[1]) / (x[2] - x[1]) - yp1);
		}
		for (int i = 2; i <= n - 1; i++) {
			sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1]);
			p = sig * y2[i - 1] + 2.0;
			y2[i] = (sig - 1.0) / p;
			u[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1]);
			u[i] = (6.0 * u[i] / (x[i + 1] - x[i - 1]) - sig * u[i - 1]) / p;
		}
		if (ypn > 0.99e30)
			qn = un = 0.0;
		else {
			qn = 0.5;
			un = (3.0 / (x[n] - x[n - 1])) * (ypn - (y[n] - y[n - 1]) / (x[n] - x[n - 1]));
		}
		y2[n] = (un - qn * u[n - 1]) / (qn * y2[n - 1] + 1.0);
		for (int k = n - 1; k >= 1; k--)
			y2[k] = y2[k] * y2[k + 1] + u[k];

	}

	/**
	 * Same as above (+1 offset arrays) & y=double[1] is used to pass out the
	 * interpolated value (y=f(x)).
	 * 
	 * @param xa
	 * @param ya
	 * @param y2a
	 * @param n
	 * @param x
	 * @param y
	 */
	private static void splint(double[] xa, double[] ya, double[] y2a, int n, double x, double[] y) {

		int klo, khi, k;
		double h, b, a;

		klo = 1;
		khi = n;
		while (khi - klo > 1) {
			k = (khi + klo) >> 1;
			if (xa[k] > x)
				khi = k;
			else
				klo = k;
		}
		h = xa[khi] - xa[klo];
		// if (h == 0.0) nrerror("Bad XA input to routine SPLINT");
		if (h == 0.0)
			System.out.format("Bad XA input to routine SPLINT\n");

		a = (xa[khi] - x) / h;
		b = (x - xa[klo]) / h;
		// *y=a*ya[klo]+b*ya[khi]+((a*a*a-a)*y2a[klo]+(b*b*b-b)*y2a[khi])*(h*h)/6.0;
		y[0] = a * ya[klo] + b * ya[khi] + ((a * a * a - a) * y2a[klo] + (b * b * b - b) * y2a[khi]) * (h * h) / 6.0;
	}

	/**
	 * Interpolate using a cubic spline.
	 * 
	 * Interpolate measured Y[X] to the Y[Z]<br>
	 * We know Y[X] = Y at values of X <br>
	 * We want Y[Z] = Y interpolated to values of Z<br>
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return
	 */
	public static double[] interpolate(double[] X, double[] Y, double[] Z) {

		double[] interpolatedValues = new double[Z.length];

		int n = X.length;

		double[] tmpY = new double[n + 1];
		double[] tmpX = new double[n + 1];

		// Create offset (+1) arrays to use with Num Recipes interpolation
		// (spline.c)
		for (int i = 0; i < n; i++) {
			tmpY[i + 1] = Y[i];
			tmpX[i + 1] = X[i];
		}
		double[] y2 = new double[n + 1];
		spline(tmpX, tmpY, n, 0., 0., y2);

		double[] y = new double[1];

		for (int i = 0; i < Z.length; i++) {
			splint(tmpX, tmpY, y2, n, Z[i], y);
			interpolatedValues[i] = y[0];
		}
		return interpolatedValues;
	}

	
	/**
	 * Rotate orthogonal channels to North and East. This method returns
	 * indeterminate results if either azimuth is < 0 or >=360 degrees
	 * 
	 * RT ticket 11466 exists for cleaning this method.
	 *
	 * @param az1
	 *            the azimuth for x
	 * @param az2
	 *            the azimuth for y
	 * @param x
	 *            input data
	 * @param y
	 *            input data
	 * @param n
	 *            the rotated n data
	 * @param e
	 *            the rotated e data
	 */
	public static void rotate_xy_to_ne(double az1, double az2, double[] x, double[] y, double[] n, double[] e) {

		/*
		 * INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and
		 * use a single azimuth to rotate We'll check the azimuths and flip
		 * signs to put channel1 to +N half and channel 2 to +E
		 */

		double azimuth = -999;
		int sign1 = 1;
		int sign2 = 1;
		if (az1 >= 0 && az1 < 90) {
			azimuth = az1;
		} else if (az1 >= 90 && az1 < 180) {
			azimuth = az1 - 180;
			sign1 = -1;
		} else if (az1 >= 180 && az1 < 270) {
			azimuth = az1 - 180;
			sign1 = -1;
		} else if (az1 >= 270 && az1 < 360) {
			azimuth = az1 - 360;
		} else {
			logger.error("MetricData.createRotatedChannels(): Don't know how to rotate az1={}", az1);
		}

		sign2 = 1;
		if (az2 >= 0 && az2 < 180) {
			sign2 = 1;
		} else if (az2 >= 180 && az2 < 360) {
			sign2 = -1;
		} else {
			logger.error("MetricData.createRotatedChannels(): Don't know how to rotate az2={}\n", az2);
		}

		double cosAz = Math.cos(azimuth * Math.PI / 180);
		double sinAz = Math.sin(azimuth * Math.PI / 180);

		for (int i = 0; i < x.length; i++) {
			n[i] = sign1 * x[i] * cosAz - sign2 * y[i] * sinAz;
			e[i] = sign1 * x[i] * sinAz + sign2 * y[i] * cosAz;
		}
	}
}
