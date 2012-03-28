package asl.azimuth;

import javax.swing.SwingWorker;

import asl.worker.Worker;

/**
 * Provides a wrapper for the InvertAzimuth class which can be used
 * as a SwingWorker or a thread.
 * 
 * @author  Joel Edwards
 *
 */
public class AzimuthLocator 
extends SwingWorker<AzimuthResult, Integer> 
implements Worker
{
    private InvertAzimuth engine = null;

    /**
     * Constructor which accepts the channel time series and initializes the
     * azimuth locator.
     *
     * @param   north       North channel time series.
     * @param   east        East channel time series.
     * @param   reference   Reference channel time series.
     */
    public AzimuthLocator(double[] north, double[] east, double[] reference)
    {
        super();
        engine = new InvertAzimuth(north, east, reference);
    }

    /**
     * Implements SwingWorker's doInBackground method. 
     *
     * @return  the result of the azimuth determination
     */
    public AzimuthResult doInBackground () 
    throws AzimuthConvergenceException
    {
        AzimuthResult result = null;

        try {
            engine.process(this);
        } catch (AzimuthInversionHalted e) {
            System.out.println("Azimuth Inversion Halted");
            ; // Cancelled
        } catch (AzimuthConvergenceException e) {
            throw e;
        }

        if (engine.getSuccess()) {
            result = new AzimuthResult();
            result.setCorrelations(engine.getCorrelations());
            result.setThetas(engine.getThetas());
            result.setOffsets(engine.getOffsets());
            result.setStandardDeviation(engine.getStandardDeviation());
            result.setBestTheta(engine.getBestTheta());
            result.setMeanOfBestCorrelations(engine.getMeanOfBestCorrelations());
            this.setProgress(100);
        }

        return result;
    }

    /**
     * Propagates the current progress to the class invoker
     * @param progress  0-100  (should not set to 100 unless done)
     */
    public void setProgressPercent(int progress)
    {
        System.out.println("New Progress: " + progress);
        this.setProgress(progress);
    }

    /**
     * Allows invoker to query the current progress
     * @return int between 0-100
     */
    public int getProgressPercent()
    {
        return getProgress();
    }

    /**
     * Allows invoker to cancel the process
     */
    public void cancel()
    {
        cancel(true);
    }

    /**
     * Allows invoker to query if the class was canceled
     * @return true if canceled state is true
     */
    public boolean cancelled()
    {
        return isCancelled();
    }

    /**
     * Query to see if the class process was successfully completed
     * @return true if Azimuth was successfully determined
     */
    public boolean getSuccess ()
    {
        return engine.getSuccess();
    }
}

