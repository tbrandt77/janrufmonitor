package de.janrufmonitor.ui.jface.application.tracer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractApplication;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class CallSimulator extends AbstractApplication implements IEventSender, IEventReceiver {

	private String NAMESPACE = "ui.jface.application.tracer.CallSimulator";

	private String LAST_CAPI_MSG_FILE = "last_capi.raw";
	private boolean isRaw;
	
	private IRuntime m_runtime;
	private ICall m_currentIdentifiedCall;
	
	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getID() {
		return "CallSimulator";
	}
	
	protected Control createContents(Composite parent) {
		String _calling = "";
		String _called = "";
		String _cip = "";
		
		File rawDataFile = new File(PathResolver.getInstance(this.getRuntime()).getDataDirectory() + this.LAST_CAPI_MSG_FILE);
		if (rawDataFile.exists() && rawDataFile.isFile()) {
			try {
				FileInputStream in = new FileInputStream(rawDataFile);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				Stream.copy(new BufferedInputStream(in), bos, true);
				String rawData = bos.toString();
				StringTokenizer st = new StringTokenizer(rawData, ";");
				if (st.countTokens()==3) {
					_calling = st.nextToken().trim();
					_called = st.nextToken().trim();
					_cip = st.nextToken().trim();
					isRaw = true;
				} else {
					this.m_logger.warning("Invalid CAPI raw format: "+rawData);
				}
			} catch (FileNotFoundException e) {
				this.m_logger.severe("Error while reading raw data file: "+e.getMessage());
			} catch (IOException e) {
				this.m_logger.severe("Error while reading raw data file: "+e.getMessage());
			}
		}
		
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

		Group simulateGroup = new Group(composite, SWT.NONE);
		simulateGroup.setLayout(new GridLayout(4, true));
		
		simulateGroup.setText(this.getI18nManager().getString(this.NAMESPACE, "simulategroup", "label", this.getLanguage()));
		
		GridData gd = new GridData();
		gd.horizontalSpan = 4;
		Label simulateText = new Label(simulateGroup, 0);
		simulateText.setText(this.getI18nManager().getString(this.NAMESPACE, "simulategroup", "description", this.getLanguage()));
		simulateText.setLayoutData(gd);
		
		Label numberLabel = new Label(simulateGroup, 0);
		numberLabel.setText(this.getI18nManager().getString(this.NAMESPACE, "number", "label", this.getLanguage()));
		
		Label msnLabel = new Label(simulateGroup, 0);
		msnLabel.setText(this.getI18nManager().getString(this.NAMESPACE, "msn", "label", this.getLanguage()));
		
		Label cipLabel = new Label(simulateGroup, 0);
		cipLabel.setText(this.getI18nManager().getString(this.NAMESPACE, "cip", "label", this.getLanguage()));

		new Label(simulateGroup, 0);
		
		gd = new GridData();
		gd.widthHint = 100;
		final Text number = new Text(simulateGroup, SWT.BORDER);
		number.setLayoutData(gd);
		number.setText(_calling);
		number.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				isRaw = false;				
			}}
		);
		
		final Combo msn = new Combo (simulateGroup, SWT.DROP_DOWN);
		
		String[] msns = PIMRuntime.getInstance().getMsnManager().getMsnList();
		String[] msnList = new String[msns.length];
		for (int i=0;i<msns.length;i++) {
			String msnalias = msns[i] + " ("+PIMRuntime.getInstance().getMsnManager().getMsnLabel(msns[i]) +")";
			msnList[i] = msnalias;
			msn.setData(msnalias, msns[i]);
		}
		msn.setItems(msnList);
		msn.setLayoutData(gd);
		msn.setText(_called);
		msn.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				isRaw = false;				
			}}
		);
		
		gd = new GridData();
		gd.horizontalSpan = 2;
	    final Combo cip = new Combo (simulateGroup, SWT.READ_ONLY);
		String[] cips = this.getRuntime().getCipManager().getCipList();
		String[] cipList = new String[cips.length];
		int marked = 0;
		for (int i=0;i<cips.length;i++) {
			String cipalias = PIMRuntime.getInstance().getCipManager().getCipLabel(cips[i], this.getLanguage());
			cipList[i] = cipalias;
			if (cips[i].equalsIgnoreCase(_cip))
				marked = i;
			cip.setData(cipalias, cips[i]);
		}
		cip.setItems(cipList);	
		cip.setLayoutData(gd);
		cip.select(marked);
				
		Button send = new Button(simulateGroup, 0);	
		final Button send2 = new Button(simulateGroup, 0);	
		final Button send3 = new Button(simulateGroup, 0);	
		send.setText(this.getI18nManager().getString(this.NAMESPACE, "sendbutton", "label", this.getLanguage()));
		
		send.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.widget instanceof Button) {
						if (msn.getText().length()>0) {
							String scip = cip.getItem(cip.getSelectionIndex());
							scip = (String) cip.getData(scip);
							
							String smsn = msn.getText();
							if (msn.getSelectionIndex()>-1) {
								smsn = msn.getItem(msn.getSelectionIndex());
						      	if (msn.getData(smsn)!=null)
						      		smsn = (String) msn.getData(smsn);		
							}       
							
							sendEvent(number.getText(), smsn, scip);
							send2.setEnabled(true);
							send3.setEnabled(true);
						}
					}	
				}
			}
		);	
		
		send2.setText(this.getI18nManager().getString(this.NAMESPACE, "clirbutton", "label", this.getLanguage()));
		
		send2.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.widget instanceof Button) {
						sendClirEvent();
						send2.setEnabled(false);
						send3.setEnabled(false);
					}	
				}
			}
		);	
		
		
		send3.setText(this.getI18nManager().getString(this.NAMESPACE, "acceptbutton", "label", this.getLanguage()));
		
		send3.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.widget instanceof Button) {
						if (msn.getText().length()>0) {
							sendAcceptEvent();
							send2.setEnabled(false);
							send3.setEnabled(false);
						}
					}	
				}
			}
		);	
		send2.setEnabled(false);
		send3.setEnabled(false);
		
		return composite;
	}
	
	protected void sendAcceptEvent() {
		if (this.m_currentIdentifiedCall!=null) {
			IEventBroker evtBroker = this.getRuntime().getEventBroker();
			evtBroker.register(this);
			
			this.m_currentIdentifiedCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_CALLACCEPTED)));
			this.m_currentIdentifiedCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_ACCEPTED));
			
			IEvent ev = evtBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED,this.m_currentIdentifiedCall);
			evtBroker.send(this, ev);    
			evtBroker.unregister(this);
			evtBroker.unregister(this, evtBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		}
	}
	
	protected void sendClirEvent() {
		if (this.m_currentIdentifiedCall!=null) {
			IEventBroker evtBroker = this.getRuntime().getEventBroker();
			evtBroker.register(this);
			
			this.m_currentIdentifiedCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_CALLCLEARED)));
			this.m_currentIdentifiedCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_MISSED));
			
			IEvent ev = evtBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED,this.m_currentIdentifiedCall);
			evtBroker.send(this, ev);    
			evtBroker.unregister(this);
			evtBroker.unregister(this, evtBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		}
	}

	protected void sendEvent(String number, String msn, String cip) {
		if (number!=null && number.length()>0)
			number = PhonenumberAnalyzer.getInstance(this.getRuntime()).toCallable(number);
		
		if (!number.startsWith("0") && isRaw){ 
			number = "0" + number;
		}
		
		IEventBroker evtBroker = this.getRuntime().getEventBroker();
		evtBroker.register(this);
		evtBroker.register(this, evtBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		
		IPhonenumber phone = PhonenumberAnalyzer.getInstance(this.getRuntime()).toClirPhonenumber(number);
		
		if (phone==null) phone = PhonenumberAnalyzer.getInstance(this.getRuntime()).toInternalPhonenumber(number, msn);
		
		if (phone==null) phone = PhonenumberAnalyzer.getInstance(this.getRuntime()).toPhonenumber(number, msn);

		IName name = PIMRuntime.getInstance().getCallerFactory().createName("","");
		ICaller aCaller = PIMRuntime.getInstance().getCallerFactory().createCaller(name, phone);
		ICip ocip = PIMRuntime.getInstance().getCallFactory().createCip(cip, "");
		IMsn omsn = PIMRuntime.getInstance().getCallFactory().createMsn(msn, "");
		ICall currentCall = PIMRuntime.getInstance().getCallFactory().createCall(aCaller,omsn,ocip);
		currentCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_MISSED));
	
		IEvent ev = evtBroker.createEvent(IEventConst.EVENT_TYPE_INCOMINGCALL,currentCall);
		evtBroker.send(this, ev);    
		evtBroker.unregister(this);
	}
	
	public String getSenderID() {
		return this.getID();
	}

	public int getPriority() {
		return 9999;
	}

	public String getReceiverID() {
		return this.getID();
	}

	public void received(IEvent event) {
		if (event!=null && event.getData() instanceof ICall && event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL) {
			this.m_currentIdentifiedCall = (ICall) event.getData();
		}		
	}
}
