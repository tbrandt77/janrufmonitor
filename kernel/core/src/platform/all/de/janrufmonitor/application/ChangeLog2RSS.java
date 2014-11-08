package de.janrufmonitor.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringEscapeUtils;

public class ChangeLog2RSS {
	/**
	 * 
	 <?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <title>jAnrufmonitor Changelog</title>
    <link>http://www.janrufmonitor.de/downloads.html</link>
    <atom:link type="application/rss+xml" href="http://www.janrufmonitor.de/changes.rss" rel="self"/>
    <description>Neuigkeiten und &Auml;nderungen zur Software jAnrufmonitor.</description>
    <language>de-de</language>
    <ttl>120</ttl>
  <item>
    <title>janrufmonitor: Ab sofort steht der Download der neuen Version 5.0.27 zur Verf&#252;gung. Die &#196;nderungen sind unter http://k-urz.de/3 zu finden.</title>
    <description>janrufmonitor: Ab sofort steht der Download der neuen Version 5.0.27 zur Verf&#252;gung. Die &#196;nderungen sind unter http://k-urz.de/3 zu finden.</description>
    <pubDate>Tue, 30 Nov 2010 15:12:57 +0000</pubDate>
    <guid>http://twitter.com/janrufmonitor/statuses/9625939948339200</guid>
    <link>http://twitter.com/janrufmonitor/statuses/9625939948339200</link>
    <twitter:source>web</twitter:source>
    <twitter:place/>
  </item>
  </rss>

	 * @param args
	 */

	public static void main(String[] args) {
		if (args.length!=1) return;
		
		String filename = args[0];
		File f = new File(filename);
		if (f.exists()) {
			try {
				StringBuffer output = new StringBuffer();
				output.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");output.append(IJAMConst.CRLF);
				output.append("<rss version=\"2.0\">");output.append(IJAMConst.CRLF);
				output.append("<channel>");output.append(IJAMConst.CRLF);
				output.append("<title>jAnrufmonitor Changelog</title>");output.append(IJAMConst.CRLF);
				output.append("<link>http://www.janrufmonitor.de/changes.rss</link>");output.append(IJAMConst.CRLF);
				output.append("<image><url>http://www.janrufmonitor.de/files/images/logo.gif</url></image>");output.append(IJAMConst.CRLF);
				output.append("<description>Neuigkeiten und Anpassungen zur Software jAnrufmonitor.</description>");output.append(IJAMConst.CRLF);
				output.append("<language>de-de</language>");output.append(IJAMConst.CRLF);
				output.append("<ttl>60</ttl>");output.append(IJAMConst.CRLF);

				FileInputStream fin = new FileInputStream(f);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				Stream.copy(fin, bos, true);
				String content = new String(bos.toByteArray(), "UTF-8");
				String[] blocks = content.split("===");
				String title = null;
				String spubDate = null;
				String description = null;
				for (int i=0;i<blocks.length;i++) {
					if (blocks[i].indexOf("(")>0) {
						title = StringEscapeUtils.escapeXml(blocks[i].substring(0, blocks[i].indexOf("(")).trim());
						spubDate = blocks[i].substring(blocks[i].indexOf("(")+1, blocks[i].indexOf(")"));
						SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
						Date pubDate = sfd.parse(spubDate);	
						sfd.applyPattern("E, dd MMM yyyy HH:mm:ss Z");
						spubDate = sfd.format(pubDate);
						description = blocks[i].substring(blocks[i].indexOf("---\n")+5, blocks[i].length()-1).trim();
						description = toHtml(description);
						output.append("<item>");output.append(IJAMConst.CRLF);
						output.append("<title>");
						output.append(title);
						output.append("</title>");output.append(IJAMConst.CRLF);
						output.append("<pubDate>");
						output.append(spubDate);
						output.append("</pubDate>");output.append(IJAMConst.CRLF);
						output.append("<description>");
						output.append(description);
						output.append("</description>");output.append(IJAMConst.CRLF);
//						output.append("<guid>http://www.janrufmonitor.de/changes.rss/");
//						output.append(pubDate.getTime());
//						output.append("</guid>");
						output.append("</item>");output.append(IJAMConst.CRLF);
					}
				}
				output.append("</channel>");output.append(IJAMConst.CRLF);
				output.append("</rss>");
				FileOutputStream fos = new FileOutputStream(f.getParent() +"/"+ f.getName().substring(0, f.getName().length()-3)+"rss");
				ByteArrayInputStream bin = new ByteArrayInputStream(output.toString().getBytes());
				Stream.copy(bin, fos, true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String toHtml(String c) throws Exception {
		String[] lines = c.split(IJAMConst.CRLF);
		boolean isList = false;
		StringBuffer s = new StringBuffer(c.length());
		s.append(StringEscapeUtils.escapeXml("<img src=\"http://www.janrufmonitor.de/files/images/logo.gif\" alt=\"jAnrufmonitor Logo\"/>"));
		for (int i=0;i<lines.length;i++) {
			if (lines[i].trim().length()==0) {
				if (isList) {
					s.append(StringEscapeUtils.escapeXml("</ul>"));
				}
				isList = false;
				continue;
			}
			if (!lines[i].startsWith("-") && !lines[i].startsWith("jAnrufmonitor")) {
				if (!isList) {
					s.append(StringEscapeUtils.escapeXml("<p>"));
					s.append(StringEscapeUtils.escapeXml(lines[i].trim()));
					s.append(StringEscapeUtils.escapeXml("</p>"));
				}
			}
			if (lines[i].startsWith("-") && !lines[i].startsWith("-----")) {
				if (!isList) {
					s.append(StringEscapeUtils.escapeXml("<ul>"));
				}
				s.append(StringEscapeUtils.escapeXml("<li>"));
				s.append(StringEscapeUtils.escapeXml(lines[i].substring(1).trim()));
				s.append(StringEscapeUtils.escapeXml("</li>"));
				isList = true;
			}
		}
		
		return s.toString();
	}

}
