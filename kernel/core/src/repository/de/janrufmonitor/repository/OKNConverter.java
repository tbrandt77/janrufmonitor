package de.janrufmonitor.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

public class OKNConverter {

	public OKNConverter() {
		super();
	}
	
	public void go() {
		// http://www.bundesnetzagentur.de/cln_1911/DE/Sachgebiete/Telekommunikation/Unternehmen_Institutionen/Nummerierung/Rufnummern/UebersichtNrnRaum_Basepage.html?nn=268384
		File db = new File("/Users/brandtt/Desktop/okn.txt");
		Properties m = new Properties();
		try {
			FileReader dbReader = new FileReader(db);
			BufferedReader bufReader = new BufferedReader(dbReader);
			String line = null;
			
			while (bufReader.ready()) {
				line = bufReader.readLine();
				StringTokenizer st = new StringTokenizer(line, ";");
				if (st.countTokens()==2) {
					m.put(st.nextToken().trim(), st.nextToken().trim());
				}
			}
			bufReader.close();
			dbReader.close();
			
			FileOutputStream os = new FileOutputStream(db.getAbsolutePath()+".new");
			m.store(os, "");
			os.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new OKNConverter().go();
	}
}
