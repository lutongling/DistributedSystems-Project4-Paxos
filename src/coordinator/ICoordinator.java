package coordinator;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface ICoordinator extends Remote {

  /**
   * Add server information.
   * @param hostName given server host name
   * @param portNum given server port number
   * @throws RemoteException
   */
  void addServer(String hostName, int portNum) throws RemoteException;

  /**
   * Return server information.
   * @return
   * @throws RemoteException
   */
  Set<Map.Entry<String, Integer>> getServer() throws RemoteException;

}
