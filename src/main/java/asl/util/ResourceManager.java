package asl.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * ResourceManager is used for loading and saving serialized objects. It may be
 * used for loading basic resources as well.
 * 
 * @author James Holland - USGS
 *
 */
public abstract class ResourceManager {

	/**
	 * Compress using GZIPOutputStream and write to file specific in fileName.
	 * 
	 * @param object
	 *            object to be serialized
	 * @param fileName
	 *            file to save object into
	 * @throws IOException
	 *             if any errors occur the caller is expected to handle them.
	 */
	public static void compressAndSaveObject(Object object, String fileName) throws IOException {
		FileOutputStream fos = null;
		GZIPOutputStream gzos = null;
		ObjectOutputStream oos = null;

		try {
			fos = new FileOutputStream(fileName);
			gzos = new GZIPOutputStream(fos);
			oos = new ObjectOutputStream(gzos);
			oos.writeObject(object);
		} finally { // This is still executed despite return statement.
			oos.close();
		}
	}

	/**
	 * Loads a resource based on the passed name.
	 * 
	 * @param fileName
	 * @return decompressed object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object loadCompressedObject(String fileName) throws IOException, ClassNotFoundException {
		Object object = null;
		GZIPInputStream gzis = null;
		ObjectInputStream ois = null;
		try {
			gzis = new GZIPInputStream(ResourceManager.class.getResourceAsStream(fileName));
			ois = new ObjectInputStream(gzis);
			object = ois.readObject();

			return object;

		} finally { // This is still executed despite return statement.
			ois.close();
		}

	}
	
	/**
	 * Returns the path of a resource directory. This assume the passed parameter is a directory.
	 * 
	 * @param directory the resource directory we want to get the path for
	 * @return the path as a string with ending /
	 */
	public static String getDirectoryPath(String directory)
	{
		return ResourceManager.class.getResource(directory).getPath()+"/";
	}
}
