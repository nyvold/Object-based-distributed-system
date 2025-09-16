package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyInterface extends Remote {
    // Remote methods should declare RemoteException
    int registerServer(String address, int port, String bindingName, ServerInterface serverStub) throws RemoteException;
    public ServerConnection connectToServer(int zone) throws RemoteException;

}
