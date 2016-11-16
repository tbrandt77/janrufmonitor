package de.janrufmonitor.application;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class Unregister {

	public static void main(String[] args) {
		System.out.println("Unregistering jAnrufmonitor from Update-Service...");
		File rkey = new File(PathResolver.getInstance().getConfigDirectory(), ".rkey");
		if (rkey.exists()) {
			System.out.println("Found .rkey file...");
			try {
				FileInputStream in = new FileInputStream(rkey);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Stream.copy(in, out, true);
				String key = out.toString();
				System.out.println("Unregistering installation with key "+key);
				URL url = new URL("https://downloads.janrufmonitor.de/registry/unregister.php?k="+key);
				URLConnection c = url.openConnection();
				c.connect();
				c.getContent();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Not found .rkey file "+rkey.getAbsolutePath());
		}
	}

}
