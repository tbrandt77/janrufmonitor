package de.janrufmonitor.service.journaling;

import java.util.List;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.filter.UUIDFilter;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;

public class Journaling extends AbstractReceiverConfigurableService implements IEventSender {
    
    private String ID = "Journaling";
    private String NAMESPACE = "service.Journaling";

	private IRuntime m_runtime;
    
    public Journaling() {
        super();
        this.getRuntime().getConfigurableNotifier().register(this);
    }
    
    public String getNamespace() {
        return this.NAMESPACE;
    }

	public String getID() {
		return this.ID;
	}
    
	public void shutdown() {
		super.shutdown();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));        
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_MANUALCALLACCEPTED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM));
		eventBroker.unregister(this);
		this.m_logger.info("Journaling is shut down ...");
	}
    
	public void startup() {
		super.startup();

		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_MANUALCALLACCEPTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM));
		eventBroker.register(this);
		this.m_logger.info("Journaling is started ...");
	}

	public void receivedOtherEventCall(IEvent event) {
		if(event.getType() == IEventConst.EVENT_TYPE_CALLACCEPTED ||
		   event.getType() == IEventConst.EVENT_TYPE_CALLCLEARED ||
		   event.getType() == IEventConst.EVENT_TYPE_MANUALCALLACCEPTED || 
		   event.getType() == IEventConst.EVENT_TYPE_CALLREJECTED ||
		   event.getType() == IEventConst.EVENT_TYPE_CALLMARKEDSPAM ||
		   event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED) {
			
			// checks wether this service is available for the incoming MSN or not.
			ICall updateCall = (ICall)event.getData();
			if (updateCall==null) {
				this.m_logger.warning("Call reference is null.");
				return;
			}
			if (this.getRuntime().getRuleEngine().validate(this.ID, updateCall.getMSN(), updateCall.getCIP(), updateCall.getCaller().getPhoneNumber())) {
				List callManagerList = this.getRuntime().getCallManagerFactory().getAllCallManagers();
				ICallManager icm = null;
				IEventBroker eventBroker = this.getRuntime().getEventBroker();
				for (int i = 0; i < callManagerList.size(); i++) {
					icm = (ICallManager) callManagerList.get(i);

					// check if the repository manager allows read/write access
					if (icm.isActive() && icm.isSupported(IWriteCallRepository.class)) {
						
						// try to keep old attribute information of call and caller
						if (icm.isSupported(IReadCallRepository.class)) {
							this.m_logger.info("Call manager <"+icm.getManagerID()+"> is supporting read mode.");
							ICallList cl = ((IReadCallRepository)icm).getCalls(new UUIDFilter(new String[]{ updateCall.getUUID() }));
							if (cl.size()==1) {
								this.m_logger.info("Found exact 1 old call in call manager <"+icm.getManagerID()+"> with UUID " + updateCall.getUUID());
								ICall oldCall = cl.get(0);
								if (oldCall != null) {
									this.m_logger.info("Setting old call info : " + oldCall + " to new call : " + updateCall);
									oldCall.getCaller().getAttributes().addAll(updateCall.getCaller().getAttributes());
									oldCall.getAttributes().addAll(updateCall.getAttributes());
									updateCall = oldCall;
									this.m_logger.info("Updated new call : " + updateCall);
								}
							}
						}
						
						((IWriteCallRepository)icm).updateCall(updateCall);
						this.m_logger.info("Call update sent to repository manager <" + icm.getManagerID() + ">: " + updateCall);
						eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL, updateCall));
					}
				}
			}
		}
	}

	public void receivedValidRule(ICall aCall) {
		List callManagerList = this.getRuntime().getCallManagerFactory().getAllCallManagers();
		ICallManager icm = null;
		for (int i = 0; i < callManagerList.size(); i++) {
			icm = (ICallManager) callManagerList.get(i);

			// check if the repository manager allows read/write access
			if (icm.isActive() && icm.isSupported(IWriteCallRepository.class)) {
				((IWriteCallRepository)icm).setCall(aCall);
				this.m_logger.info("Call sent to repository manager <" + icm.getManagerID() + ">: " + aCall);
			}
		}
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getSenderID() {
		return this.getID();
	}

}
