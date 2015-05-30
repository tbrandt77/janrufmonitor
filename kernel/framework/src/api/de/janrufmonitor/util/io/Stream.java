package de.janrufmonitor.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class provides support for stream copy capabilities
 * 
 *@author     Thilo Brandt
 *@created    2005/11/03
 */
public class Stream {

	/**
	 * Copy an inputstream into an outputstream with the specified buffer size. Both streams
	 * are finalized (closed) if the finalize flag is set to true.
	 * 
	 * @param in InputStream object (could also be buffered)
	 * @param out OutputStream object
	 * @param finalize true if both streams should be closed after the copy
	 * @param bufSize size of the copy buffer
	 * @throws IOException
	 */
	public synchronized static void copy(InputStream in, OutputStream out, boolean finalize, int bufSize) throws IOException {
		byte[] buffer = new byte[bufSize];
		int bytesRead;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}  
		out.flush();
		if (finalize) {
			in.close();
			out.close();
		}
	}
	
	/**
	 * Read the first n bytes from the InputStream to the OutputStream.
	 * 
	 * @param in InputStream object (could also be buffered)
	 * @param out OutputStream object
	 * @param n number of bytes to read (max. length: Short.MAX_VALUE)
	 * @throws IOException
	 */
	public synchronized static void first(InputStream in, OutputStream out, int n) throws IOException {
		byte[] buffer = new byte[Short.MAX_VALUE];
		int bytesRead;
		int total = 0;
		while (total<n && (bytesRead = in.read(buffer, 0 , n)) != -1) {
			total += bytesRead;
			out.write(buffer, 0, bytesRead);
		}  
		out.flush();
		in.close();
		out.close();
	}
	
	/**
	 * Copy an inputstream into an outputstream without closing the streams and with the default buffer size
	 * of Short.MAX_VALUE (32767 bytes)
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out) throws IOException {
		Stream.copy(in, out, false, Short.MAX_VALUE);
	}
	
	/**
	 * Copy an inputstream into an outputstream with the default buffer size
	 * of Short.MAX_VALUE (32767 bytes). Both streams are finalized (closed) if the finalize flag is set to true.
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out, boolean finalize) throws IOException {
		Stream.copy(in, out, finalize, Short.MAX_VALUE);
	}
	
	/**
	 * Copy an inputstream into an outputstream without closing the streams and 
	 * with the specified buffer size.
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out, int bufSize) throws IOException {
		Stream.copy(in, out, false, bufSize);
	}
}
