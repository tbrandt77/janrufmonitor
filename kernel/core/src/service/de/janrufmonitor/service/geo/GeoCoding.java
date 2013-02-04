package de.janrufmonitor.service.geo;

import java.util.List;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.service.IModifierService;
import de.janrufmonitor.util.math.Point;

public class GeoCoding extends AbstractReceiverConfigurableService implements IEventSender, IModifierService {

    private static String ID = "GeoCoding";
    private static String NAMESPACE = "service.GeoCoding";

	private IRuntime m_runtime;
	
	public GeoCoding() {
        super();
        this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return GeoCoding.ID;
	}

	public String getNamespace() {
		return GeoCoding.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getSenderID() {
		return GeoCoding.ID;
	}

	public void shutdown() {
		super.shutdown();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.unregister(this);
		this.m_logger.info("GeoCoding is shut down ...");
	}
    
	public void startup() {
		super.startup();

		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.register(this);
		this.m_logger.info("GeoCoding is started ...");
	}

	public void receivedValidRule(ICall call) {
		if (call==null) return;
		
		if (call.getCaller().getPhoneNumber().isClired()) return;
		
		ICaller caller = call.getCaller();
		if (caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)!=null && caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)!=null) {
			this.m_logger.info("Call already set with geo data ...");
			return;
		}
		
		Point p = GeoCoder.getInstance().getCoordinates(caller.getAttributes());
		if (p!=null) {
			caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC, Integer.toString(p.getAccurance())));
			caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT, Double.toString(p.getLatitude())));
			caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG, Double.toString(p.getLongitude())));
			
			List callManagerList = this.getRuntime().getCallManagerFactory().getAllCallManagers();
			ICallManager icm = null;
			IEventBroker eventBroker = this.getRuntime().getEventBroker();
			for (int i = 0; i < callManagerList.size(); i++) {
				icm = (ICallManager) callManagerList.get(i);

				// check if the repository manager allows read/write access
				if (icm.isActive() && icm.isSupported(IWriteCallRepository.class)) {
					((IWriteCallRepository)icm).updateCall(call);
					this.m_logger.info("Call update sent with geocoding: " + call);
					eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL, call));
				}
			}
		} else {
			this.m_logger.info("Geocoding not successfully for call: " + call);
		}
	}

	public void modifyObject(Object o) {
		if (o instanceof ICall) {
			o = ((ICall)o).getCaller();
		}
		if (o instanceof ICaller) {
			ICaller caller = ((ICaller)o);
			if (caller.getPhoneNumber().isClired()) return;
			
			if (caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)!=null && caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)!=null) {
				this.m_logger.info("Caller already set with geo data ...");
				return;
			}
			
			Point p = GeoCoder.getInstance().getCoordinates(caller.getAttributes());
			if (p!=null) {
				caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC, Integer.toString(p.getAccurance())));
				caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT, Double.toString(p.getLatitude())));
				caller.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG, Double.toString(p.getLongitude())));
			}
		}
	}

}
