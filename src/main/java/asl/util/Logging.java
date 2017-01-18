package asl.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logging {
	
	public static String exceptionToString(Exception e){
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		return stringWriter.toString();
	}

}
