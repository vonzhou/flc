package net.floodlightcontroller.dedu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.openflow.protocol.OFFPUpdate;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionFPUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Distributing implements IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	protected Set macAddresses;
	protected static Logger logger;
	// modules about compute the finger print vector
	//===================TODO======================

	@Override
	//Need provide ID for OFMessageListener
	public String getName() {
		return Distributing.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l=
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = LoggerFactory.getLogger(Distributing.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		//want to got useful info from packet in msg
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		//i want to see the packet in data,to see if get such as the file name
		
		OFPacketIn pi = (OFPacketIn)msg;
		logger.info("PacketIn buffer id :" + pi.getBufferId());
		 
		OFMatch match = new OFMatch();
	    match.loadFromPacket(pi.getPacketData(), pi.getInPort());
	    logger.info("destination port num: " + (short)match.getTransportDestination());
	    if(match.getNetworkProtocol() == 0x11 && match.getTransportDestination() == 2500){
	    	// UDP
	    	logger.info("packet_in len: " + pi.getLengthU());
	    	StringBuffer buff = new StringBuffer();
	        for (int i = 0; i < pi.getPacketData().length; i++)	            {
	        	 buff.append((char)pi.getPacketData()[i]);
	        }
	    	String filename = buff.substring(54, buff.length());
	    	logger.info("filename : " + filename);
	    	 
	    }
	     
	    /*After got the file name from the packet_in,
	     * we push the appropriate fp vector to the SW
	     * (TODO:the switches along the path)
	     */
	    OFFPUpdate fu =
                (OFFPUpdate) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FP_UPDATE);
        OFActionFPUpdate action = new OFActionFPUpdate();
        action.setVector(0xffffffff); // just for testing
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);

        fu.setBufferId(OFFPUpdate.BUFFER_ID_NONE)
            .setActions(actions)
            .setLengthU(OFFPUpdate.MINIMUM_LENGTH+OFActionFPUpdate.MINIMUM_LENGTH);
        try {
			sw.write(fu, cntx);
		} catch (IOException e) {
            logger.error("Failure writing fp update", e);
        }
	
		return Command.CONTINUE;// other handlers in this pipeline to process this packetin ;
	}

}
