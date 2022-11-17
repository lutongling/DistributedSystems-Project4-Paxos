package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import coordinator.ICoordinator;
import utils.KVLogger;

public class ServerController {
  private static KVLogger logger = new KVLogger("ServerController");

  public static void main(String[] args) {
    if ((args.length != 3 && args.length != 5) || !args[0].matches("\\d+")) {
      logger.logErrorMessage("Please enter in valid format in either 1 or 2: ");
      logger.logErrorMessage("1. To Start a new server: java ServerController <Server Port Number> <Server host name> <Coordinator hostName>");
      logger.logErrorMessage("2. To restart a peer server from a current live server: java ServerController <To-be-restarted Server Port Number> <To-be-restarted Server Host Name> <Coordinator host name> <Live Server Port Number> <Live Server Host Name>");
      System.exit(1);
    }

    // valid CLI args: a port number
    int portNum = Integer.parseInt(args[0]);
    String hostName = args[1];
    String coordinatorHostName = args[2];

    try {
      KeyValue kv = new KeyValueStore(portNum, hostName);

      // add replica and set coordinator for this server --- connect to coordinator
      ICoordinator coordinator = (ICoordinator) Naming.lookup("rmi://" + coordinatorHostName + ":1111/KeyValueCoordinator");
      coordinator.addServer(hostName, portNum);

      if(args.length == 5) {
        int peerPortNum = Integer.parseInt(args[3]);
        String peerHostName = args[4];
        kv.reStart(peerPortNum, peerHostName);
      }

      LocateRegistry.createRegistry(portNum);
      Naming.rebind("rmi://localhost:" + portNum + "/KeyValueService", kv);

      logger.logInfoMessage("Server starts on port " + portNum);

    } catch (RemoteException | MalformedURLException | NotBoundException e) {
      logger.logErrorMessage(e.getMessage());
    }

  }
}


