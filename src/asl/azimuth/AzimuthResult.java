package asl.azimuth;

/**
 * Class for returning the Azimuth calculation results from AzimuthLocator
 * 
 * @author  Joel Edwards
 */
public class AzimuthResult 
{
    private double[] m_correlations = null;
    private double[] m_thetas = null;
    private double[] m_offsets = null;
    private double   m_standardDeviation = 0.0;
    private double   m_bestTheta = 0.0;
    private double   m_meanOfBestCorrelations = 0.0;

    /**
     * Constructor does not initialize values
     */
    public AzimuthResult()
    {
        ;
    }

    /**
     * Set the correlations array values
     * @param correlations values between -1 and +1
     */
    public void setCorrelations(double[] correlations)
    {
        m_correlations = correlations;
    }

    /**
     * Set the theta offset angle array values (degrees)
     * @param thetas Offset from reference angle in degrees
     */
    public void setThetas(double[] thetas)
    {
        m_thetas = thetas;
    }

    /**
     * Set the
     * @param offsets  Not used
     */
    public void setOffsets(double[] offsets)
    {
        m_offsets = offsets;
    }

    /**
     * Set the standard deviation in the theta array
     * @param sd Standard angle deviation in degrees
     */
    public void setStandardDeviation(double sd)
    {
        m_standardDeviation = sd;
    }

    /**
     * Set the Estimate of the best theta value for the final Angle offset in degrees
     * @param bt Best Angle offset in degrees
     */
    public void setBestTheta(double bt)
    {
        m_bestTheta = bt;
    }

    /**
     * Set the value of the Mean of the best correlation values corresponding to setBestTheta()
     * @param bc Mean of best Correlation values -1 to +1
     */
    public void setMeanOfBestCorrelations(double bc)
    {
        m_meanOfBestCorrelations = bc;
    }

    /**
     * Get array of correlation values
     * @return array of correlation values -1 to +1
     */
    public double[] getCorrelations()
    {
        return m_correlations;
    }

    /**
     * Get array of theta offset angles in degrees
     * @return array of theta offset angles in degrees
     */
    public double[] getThetas()
    {
        return m_thetas;
    }

    /**
     * Not used
     * @return not used
     */
    public double[] getOffsets()
    {
        return m_offsets;
    }

    /**
     * Get the Standard Deviation in the theta array angles
     * @return  Standard Deviation in theta array angles
     */
    public double getStandardDeviation()
    {
        return m_standardDeviation;
    }

    /**
     * Get the angle of the best theta array estimate in degrees
     * @return angle of best theta array estimate in degrees
     */
    public double getBestTheta()
    {
        return m_bestTheta;
    }

    /**
     * Get the Mean of the best Correlation values used to calculate BestTheta
     * @return Mean of best correlation values -1 to 1
     */
    public double getMeanOfBestCorrelations()
    {
        return m_meanOfBestCorrelations;
    }
}

