package net.xtapi.serviceProvider;

public interface IXTapi {

    public void callback(int dwDevice,int dwMessage, int dwInstance,
            int dwParam1,int dwParam2,int dwParam3);
	
}
