package asl.timeseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreprocessingUtils {
	private static final Logger logger = LoggerFactory.getLogger(PreprocessingUtils.class);

	/**
	 * Performs an in place cosine taper on passed array of data.
	 * 
	 * @param timeseries time series
	 * @param width width of cosine taper
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
	 * Rotate orthogonal channels to North and East. If the azimuths are more
	 * than 10 degrees from orthogonal a warning is logged and the rotation
	 * still occurs as if orthogonal. *
	 * 
	 * @param azimuthX
	 *            the azimuth for x
	 * @param azimuthY
	 *            the azimuth for y
	 * @param inputX
	 *            input data
	 * @param inputY
	 *            input data
	 * @param north
	 *            the rotated north data
	 * @param east
	 *            the rotated east data
	 */
	public static void rotate_xy_to_ne(double azimuthX, double azimuthY, double[] inputX, double[] inputY,
			double[] north, double[] east) throws TimeseriesException {

		/*
		 * INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and
		 * use a single azimuth to rotate We'll check the azimuths and flip
		 * signs to put channel1 to +N half and channel 2 to +E
		 */
		
		/*
		 * Check if the azimuths are more than 10 degrees off. Log a warning,
		 * but still rotate as if perpendicular.
		 */
		if ((Math.abs(azimuthX - azimuthY) - 90) > 10) {
			logger.warn(
					"Azimuth difference greater than 10 degrees, still rotating as if perpendicular: AzimuthX: {}  AzimuthY: {}",
					azimuthX, azimuthY);
		}

		double azimuth;
		int sign1 = 1;
		int sign2 = 1;
		if (azimuthX >= 0 && azimuthX < 90) {
			azimuth = azimuthX;
		} else if (azimuthX >= 90 && azimuthX < 180) {
			azimuth = azimuthX - 180;
			sign1 = -1;
		} else if (azimuthX >= 180 && azimuthX < 270) {
			azimuth = azimuthX - 180;
			sign1 = -1;
		} else if (azimuthX >= 270 && azimuthX <= 360) {
			azimuth = azimuthX - 360;
		} else {
			throw new TimeseriesException(
					"Don't know how to rotate az1=" + azimuthX);
		}

		sign2 = 1;
		if (azimuthY >= 0 && azimuthY < 180) {
			sign2 = 1;
		} else if (azimuthY >= 180 && azimuthY < 360) {
			sign2 = -1;
		} else {
			throw new TimeseriesException(
					"Don't know how to rotate az2=" + azimuthY);
		}

		double cosAz = Math.cos(azimuth * Math.PI / 180);
		double sinAz = Math.sin(azimuth * Math.PI / 180);

		for (int i = 0; i < inputX.length; i++) {
			north[i] = sign1 * inputX[i] * cosAz - sign2 * inputY[i] * sinAz;
			east[i] = sign1 * inputX[i] * sinAz + sign2 * inputY[i] * cosAz;
		}
	}
}
