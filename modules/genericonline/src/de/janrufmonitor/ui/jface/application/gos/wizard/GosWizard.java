package de.janrufmonitor.ui.jface.application.gos.wizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.installer.InstallerConst;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.gos.wizard.pages.GosPage;
import de.janrufmonitor.ui.jface.wizards.AbstractWizard;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;

public class GosWizard extends AbstractWizard {

	private String NAMESPACE = "ui.jface.application.gos.wizard.GosWizard"; 

	private IRuntime m_runtime;
	private AbstractPage[] m_pages;
	
	public GosWizard() {
		super();
		setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		this.m_pages = new AbstractPage[1];
		this.m_pages[0] = new GosPage(null);
		
		this.addPage(this.m_pages[0]);
	}


	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return GosWizard.class.getName();
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public boolean performFinish() {
		Map data = ((GosPage)this.m_pages[0]).getResult();

		String id = (String) data.get("name");
		
		// descriptor data
		Properties descriptor = new Properties();
		descriptor.setProperty(InstallerConst.DESCRIPTOR_REQUIRED_MAJOR_VERSION, IJAMConst.VERSION_MAJOR);
		descriptor.setProperty(InstallerConst.DESCRIPTOR_REQUIRED_MINOR_VERSION, IJAMConst.VERSION_MINOR);
		descriptor.setProperty(InstallerConst.DESCRIPTOR_REQUIRED_PATCH_VERSION, IJAMConst.VERSION_PATCH);
		
		descriptor.setProperty(InstallerConst.DESCRIPTOR_TYPE, "repository");
		descriptor.setProperty(InstallerConst.DESCRIPTOR_VERSION, (String) data.get("version"));
		descriptor.setProperty(InstallerConst.DESCRIPTOR_NAME, "mod-rep-"+id.toLowerCase());
		descriptor.setProperty(InstallerConst.DESCRIPTOR_NAMESPACE, "repository."+id);
		descriptor.setProperty(InstallerConst.DESCRIPTOR_RESTART, "true");
		descriptor.setProperty(InstallerConst.DESCRIPTOR_CONSOLE_ENABLED, "true");
		descriptor.setProperty(InstallerConst.DESCRIPTOR_CONFIG_OVERWRITE, "true");
		descriptor.setProperty(InstallerConst.DESCRIPTOR_REMOVE, "false");
		
		// inf data
		Properties inf = new Properties();
		inf.setProperty("repository.CallerManagerFactory:manager_"+id+":value", "de.janrufmonitor.repository.DynamicWebCallerManager");
		inf.setProperty("repository.CallerManagerFactory:manager_"+id+":access", "system");
		inf.setProperty("repository."+id+":enabled:value", "true");
		inf.setProperty("repository."+id+":enabled:type", "boolean");
		inf.setProperty("repository."+id+":usecache:value", "true");
		inf.setProperty("repository."+id+":usecache:type", "boolean");
		inf.setProperty("repository."+id+":useragent:value", "Mozilla/4.0 (compatible; MSIE; Windows NT)");
		inf.setProperty("repository."+id+":useragent:type", "text");
		inf.setProperty("repository."+id+":priority:value", "10");
		inf.setProperty("repository."+id+":priority:type", "integer");	
		inf.setProperty("repository."+id+":skipbytes:value", (String) data.get("offset"));
		inf.setProperty("repository."+id+":skipbytes:type", "integer");
		inf.setProperty("repository."+id+":url:value", (String) data.get("url"));
		inf.setProperty("repository."+id+":regexp.lastname:value", (String) data.get("lastname"));
		inf.setProperty("repository."+id+":regexp.firstname:value", (String) data.get("firstname"));
		inf.setProperty("repository."+id+":regexp.street:value", (String) data.get("street"));
		inf.setProperty("repository."+id+":regexp.streetno:value", (String) data.get("streetno"));
		inf.setProperty("repository."+id+":regexp.pcode:value", (String) data.get("postal"));
		inf.setProperty("repository."+id+":regexp.city:value", (String) data.get("city"));
		inf.setProperty("repository."+id+":regexp.areacode:value", (String) data.get("area"));
		inf.setProperty("repository."+id+":regexp.phone:value", (String) data.get("number"));
		inf.setProperty("repository."+id+":intareacode:value", (String) data.get("intarea"));
		inf.setProperty("repository."+id+":locale:value", (String) data.get("locale"));
		
		inf.setProperty("ui.jface.configuration.ConfigurationCommand:page_"+id+":value", "de.janrufmonitor.ui.jface.configuration.pages.DynamicWebCallerManager");
		inf.setProperty("ui.jface.configuration.ConfigurationCommand:page_"+id+":access", "system");

		// i18n data
		Properties i18n = new Properties();
		i18n.setProperty("repository."+id+":title:label:de", (String) data.get("name"));
		i18n.setProperty("ui.jface.configuration.pages."+id+":title:label:de", (String) data.get("name"));
		i18n.setProperty("ui.jface.configuration.pages."+id+":description:label:de", "Dieses Modul identifiziert Anrufe bei bestehender Internetverbindung mit der Online-Telefonauskunft "+(String) data.get("name"));
		i18n.setProperty("ui.jface.configuration.pages."+id+":enabled:label:de", "Datenablage aktiviert");
		i18n.setProperty("ui.jface.configuration.pages."+id+":skipbytes:label:de", "Zeichen-Offset: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":url:label:de", "Abfrage-URL: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":intareacode:label:de", "L\u00E4ndercode: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.lastname:label:de", "Muster f\u00FCr Nachname: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.firstname:label:de", "Muster f\u00FCr Vorname: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.street:label:de", "Muster f\u00FCr Strasse: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.streetno:label:de", "Muster f\u00FCr Hausnummer: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.pcode:label:de", "Muster f\u00FCr PLZ: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.city:label:de", "Muster f\u00FCr Stadt: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.areacode:label:de", "Muster f\u00FCr Vorwahl: ");
		i18n.setProperty("ui.jface.configuration.pages."+id+":regexp.phone:label:de", "Muster f\u00FCr Rufnummer: ");
		
		Properties i18n_en = new Properties();
		i18n_en.setProperty("repository."+id+":title:label:en", (String) data.get("name"));
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":title:label:en", (String) data.get("name"));
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":description:label:en", "This module identifies callers with the online service "+(String) data.get("name"));
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":enabled:label:en", "Repository enabled");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":skipbytes:label:en", "Offset: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":url:label:en", "URL: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":intareacode:label:en", "International area code: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.lastname:label:en", "Pattern for lastname: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.firstname:label:en", "Pattern for firstname: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.street:label:en", "Pattern for street: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.streetno:label:en", "Pattern for street no: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.pcode:label:en", "Pattern for postal code: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.city:label:en", "Pattern for city: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.areacode:label:en", "Pattern for areacode: ");
		i18n_en.setProperty("ui.jface.configuration.pages."+id+":regexp.phone:label:en", "Pattern for number: ");

		
		// create jam.zip archive
		File dir = new File((String) data.get("directory"));
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		File icon = new File((String) data.get("icon"));
		
		ZipArchive z = new ZipArchive(dir.getAbsolutePath() + File.separator + "mod-rep-"+id.toLowerCase()+"."+descriptor.getProperty(InstallerConst.DESCRIPTOR_VERSION, "1.0.0")+".jam.zip");
		try {
			z.open();

			// write descriptor
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			descriptor.store(bos, "");
			bos.flush();

			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
			z.add("~meta-inf/descriptor.properties", bin);
			
			// write inf data
			 bos = new ByteArrayOutputStream();
			inf.store(bos, "");
			bos.flush();

			bin = new ByteArrayInputStream(bos.toByteArray());
			z.add("install/repository."+id+".inf", bin);
			
			// write i18n data
			bos = new ByteArrayOutputStream();
			i18n.store(bos, "");
			bos.flush();

			bin = new ByteArrayInputStream(bos.toByteArray());
			z.add("install/repository."+id+".i18n", bin);
			
			// write i18n_en data
			bos = new ByteArrayOutputStream();
			i18n_en.store(bos, "");
			bos.flush();

			bin = new ByteArrayInputStream(bos.toByteArray());
			z.add("install/repository."+id+".en.i18n", bin);

			if (icon.exists())
				z.add("images/"+id+".png", new FileInputStream(icon));
			
			z.close();
			bin.close();
			bos.close();
		} catch (ZipArchiveException e) {
			this.m_logger.severe(e.toString());
		} catch (IOException e) {
			this.m_logger.severe(e.toString());
		}
		
		return true;
	}

}
