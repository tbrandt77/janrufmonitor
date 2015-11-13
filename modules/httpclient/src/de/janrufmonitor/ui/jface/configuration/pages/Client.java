package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class Client extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.Client";
	private String CONFIG_NAMESPACE_1 = "service.Client";
	private String CONFIG_NAMESPACE_2 = "service.client.RequesterFactory";

	private IRuntime m_runtime;

	private static final String thinClientEvents = IEventConst.EVENT_TYPE_APPLICATION_READY + "," + 
			IEventConst.EVENT_TYPE_INCOMINGCALL + "," + 
			IEventConst.EVENT_TYPE_CALLACCEPTED + "," + 
			IEventConst.EVENT_TYPE_CALLCLEARED + "," + 
			IEventConst.EVENT_TYPE_CALLREJECTED + "," +
			IEventConst.EVENT_TYPE_OUTGOINGCALL;
		
	private static final String fatClientEvents = IEventConst.EVENT_TYPE_APPLICATION_READY + "," +  
			IEventConst.EVENT_TYPE_IDENTIFIED_CALL + "," + 
			IEventConst.EVENT_TYPE_CALLACCEPTED + "," + 
			IEventConst.EVENT_TYPE_CALLCLEARED + "," +
			IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL;
	
	public String getConfigNamespace() {
		return this.CONFIG_NAMESPACE_1;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getParentNodeID() {
		return IConfigPage.SERVICE_NODE;
	}

	public String getNodeID() {
		return "Client".toLowerCase();
	}

	public int getNodePosition() {
		return 10;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		StringFieldEditor sfe = new StringFieldEditor(
			this.CONFIG_NAMESPACE_2+SEPARATOR+"server",
			this.m_i18n.getString(this.getNamespace(), "server", "label", this.m_language),
			20,
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
		
		sfe = new StringFieldEditor(
			this.CONFIG_NAMESPACE_2+SEPARATOR+"port",
			this.m_i18n.getString(this.getNamespace(), "port", "label", this.m_language),
			5,
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
		
		if (isExpertMode()) {
			BooleanFieldEditor bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"autoconnect",
				this.m_i18n.getString(this.getNamespace(), "autoconnect", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
			
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"autoreconnect",
				this.m_i18n.getString(this.getNamespace(), "autoreconnect", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
			
			sfe = new StringFieldEditor(
				this.CONFIG_NAMESPACE_1+SEPARATOR+"port",
				this.m_i18n.getString(this.getNamespace(), "cport", "label", this.m_language),
				5,
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(false);
			addField(sfe);
			
			RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(
				this.CONFIG_NAMESPACE_1+SEPARATOR+"events",
				this.m_i18n.getString(this.getNamespace(), "events", "label", this.m_language),
				1,
				new String[][] {
					{this.m_i18n.getString(this.getNamespace(), "thin", "label", this.m_language), thinClientEvents},
					{this.m_i18n.getString(this.getNamespace(), "fat", "label", this.m_language), fatClientEvents}
			    },
				this.getFieldEditorParent(),
				true
			);
			addField(rgfe);
			
			ComboFieldEditor cfe = new ComboFieldEditor(
				"repository.HttpCallerManager"+SEPARATOR+"remote_repository",
				this.m_i18n.getString(this.getNamespace(), "remote_ab", "label", this.m_language),
				new String[][] {
					{this.m_i18n.getString(this.getNamespace(), "callerdirectory", "label", this.m_language), "CallerDirectory"},
					{this.m_i18n.getString(this.getNamespace(), "mysqladdressbook", "label", this.m_language), "MySqlAddressbook"},
					{this.m_i18n.getString(this.getNamespace(), "outlook", "label", this.m_language), "OutlookCallerManager"},
					{this.m_i18n.getString(this.getNamespace(), "macab", "label", this.m_language), "MacAddressBookManager"}
				},
				this.getFieldEditorParent()
			);
			addField(cfe);
			
			cfe = new ComboFieldEditor(
				"repository.HttpCallManager"+SEPARATOR+"remote_repository",
				this.m_i18n.getString(this.getNamespace(), "remote_j", "label", this.m_language),
				new String[][] {
					{this.m_i18n.getString(this.getNamespace(), "defaultjournal", "label", this.m_language), "DefaultJournal"},
					{this.m_i18n.getString(this.getNamespace(), "mysqljournal", "label", this.m_language), "MySqlJournal"}
				},
				this.getFieldEditorParent()
			);
			addField(cfe);
		}
	}
	
	public boolean performOk() {
		boolean ok = super.performOk();
		
		String events = this.getPreferenceStore().getString(this.CONFIG_NAMESPACE_1+SEPARATOR+"events");
		if (events.equalsIgnoreCase(fatClientEvents)) {
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("ui.jface.application.journal.Journal", "repository", "HttpCallManager");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("ui.jface.application.editor.Editor", "repository", "HttpCallerManager");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.DefaultJournal", "enabled", "false");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.CallerDirectory", "enabled", "false");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.HttpCallerManager", "enabled", "true");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.HttpCallManager", "enabled", "true");
		}
		if (events.equalsIgnoreCase(thinClientEvents)) {
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("ui.jface.application.journal.Journal", "repository", "DefaultJournal");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("ui.jface.application.editor.Editor", "repository", "CallerDirectory");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.DefaultJournal", "enabled", "true");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.CallerDirectory", "enabled", "true");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.HttpCallerManager", "enabled", "false");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("repository.HttpCallManager", "enabled", "false");
		}
		
		this.getRuntime().getConfigurableNotifier().notifyByNamespace(this.CONFIG_NAMESPACE_2);
		this.getRuntime().getConfigurableNotifier().notifyByNamespace("ui.jface.application.journal.Journal");
		this.getRuntime().getConfigurableNotifier().notifyByNamespace("repository.DefaultJournal");
		this.getRuntime().getConfigurableNotifier().notifyByNamespace("repository.CallerDirectory");
		this.getRuntime().getConfigurableNotifier().notifyByNamespace("repository.HttpCallerManager");
		this.getRuntime().getConfigurableNotifier().notifyByNamespace("repository.HttpCallManager");
		return ok;
	}
}