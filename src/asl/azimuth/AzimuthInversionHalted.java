package asl.azimuth;

/**
 * Defines AzimuthInversionHalted exception for propagating math Inversion failure.
 * 
 * @author  Joel Edwards
 */
public class AzimuthInversionHalted 
extends Exception 
{
    public static final long serialVersionUID = 1L;

    public AzimuthInversionHalted()
    {
        super();
    }

    public AzimuthInversionHalted(String message)
    {
        super(message);
    }
}

