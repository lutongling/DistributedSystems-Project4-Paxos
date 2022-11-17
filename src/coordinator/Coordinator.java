package coordinator;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Coordinator extends UnicastRemoteObject implements ICoordinator {
  private static Set<Map.Entry<String, Integer>> serverHostNameToPortNum;

  public Coordinator() throws RemoteException {
    super();
    serverHostNameToPortNum = new HashSet<>();
  }

  @Override
  public void addServer(String hostName, int portNum) {
    Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>(hostName, portNum);
    serverHostNameToPortNum.add(entry);
  }

  @Override
  public Set<Map.Entry<String, Integer>> getServer() {
    return serverHostNameToPortNum;
  }


}
