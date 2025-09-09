package com.ass1.server;

import java.rmi.Remote;

public interface ProxyInterface extends Remote {
    // should these methods throw remote exceptions?
    public int registerServer(String address, int port, String bindingName, ServerInterface serverStub);
}
