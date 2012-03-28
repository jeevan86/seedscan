package asl.azimuth;

/**
 * Defines AzimuthConvergenceException for propagating math convergence failure.
 * 
 * @author  Joel Edwards
 */

public class AzimuthConvergenceException
extends Exception
{
    public static final long serialVersionUID = 1L;

    public AzimuthConvergenceException()
    {
        super();
    }

    public AzimuthConvergenceException(String message)
    {
        super(message);
    }
}

