package com.ass1.server;

public class ServerConnection {
    /*
     * ServerConnection object is given to the client, 
     * client looks up server i java RMI registry with adress, port and name.
     * client can then invoke methods from ServerInterface via the given server
     */
    private final String serverAddress;
    private final int serverPort;
    private final int zone;
    private final String bindingName;

    public ServerConnection(String serverAddress, int serverPort, int zone, String bindingName) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.zone = zone;
        this.bindingName = bindingName;
    }

    public String getServerAddress() { return serverAddress; }
    public int getServerPort() { return serverPort; }
    public int getZone() { return zone; }
    public String getBindingName() { return bindingName; }
}
