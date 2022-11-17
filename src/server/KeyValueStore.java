package server;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import coordinator.ICoordinator;
import utils.KVLogger;
import utils.KVOperation;

public class KeyValueStore extends UnicastRemoteObject implements KeyValue, Serializable {
  private static final long serialVersionUID = 1l;

  // server fields
  private long maxId;
  private int portNum;
  private String hostName;

  // for key value store
  private Map<String, String> dictionary;

  // for logging
  private KVLogger logger;

  // for paxos roles
  private KeyValueStore.Proposer proposer;
  private KeyValueStore.Acceptor acceptor;
  private KeyValueStore.Learner learner;

  public KeyValueStore(int portNum, String hostName) throws RemoteException {
    this.maxId = 0;
    this.portNum = portNum;
    this.hostName = hostName;
    this.dictionary = new HashMap<>();
    this.logger = new KVLogger("KeyValueStore");
    this.proposer = new KeyValueStore.Proposer();
    this.acceptor = new KeyValueStore.Acceptor();
    this.learner = new KeyValueStore.Learner();
  }

  @Override
  public boolean put(String key, String value) throws RemoteException {
    int proposalId = generateProposalId();
    ICoordinator coordinator = getCoordinator();

    boolean isPutAgreed = proposer.propose(proposalId, coordinator);

    if(isPutAgreed) {
      List<KeyValue> learners = new ArrayList<>();

      for(Map.Entry<String, Integer> server : coordinator.getServer()) {
        int port = server.getValue();
        String hostName = server.getKey();
        try {
          KeyValue peer = (KeyValue) Naming.lookup("rmi://" + hostName + ":" + port + "/KeyValueService");
          learners.add(peer);

        } catch (Exception e) {
          e.printStackTrace();
          logger.logErrorMessage("Server " + port + " crashed!");
        }
      }

      KVOperation put = new KVOperation(KVOperation.Type.PUT, key, value);

      boolean isPut = false;
      for(KeyValue learner : learners) {
        isPut = learner.doCommit(put);
      }

      return isPut;

    } else {
      return false;
    }

  }

  @Override
  public String get(String key) {
    logger.logInfoMessage("REQUEST - GET; KEY => " + key);

    if(!dictionary.containsKey(key)) {
      logger.logWarningMessage("Response => code: 404; "
              + "message: key not found");
      return null;
    }

    String val = dictionary.get(key);
    logger.logInfoMessage("Response => code: 200; "
            + "message: " + val);

    return val;
  }

  @Override
  public int delete(String key) throws RemoteException {
    int proposalId = generateProposalId();
    ICoordinator coordinator = getCoordinator();
    boolean isDeletedAgreed = proposer.propose(proposalId, coordinator);

    if(isDeletedAgreed) {
      List<KeyValue> learners = new ArrayList<>();

      for(Map.Entry<String, Integer> server : coordinator.getServer()) {
        int port = server.getValue();
        String hostName = server.getKey();
        try {
          KeyValue peer = (KeyValue) Naming.lookup("rmi://" + hostName + ":" + port + "/KeyValueService");
          learners.add(peer);

        } catch (Exception e) {
          logger.logErrorMessage("Server " + port + " crashed!");
        }
      }

      KVOperation delete = new KVOperation(KVOperation.Type.DELETE, key, null);

      boolean isDeleted = false;
      for(KeyValue learner : learners) {
        isDeleted = learner.doCommit(delete);
      }

      return isDeleted ? 200 : 404;

    } else {
      return 500;
    }


  }

  @Override
  public int getPortNum() {
    return portNum;
  }

  @Override
  public ICoordinator getCoordinator() throws RemoteException {
    ICoordinator coordinator = null;
    try {
      coordinator = (ICoordinator) Naming.lookup("rmi://localhost:1111/KeyValueCoordinator");
    } catch (NotBoundException | MalformedURLException | RemoteException e) {
      e.printStackTrace();
    }
    return coordinator;
  }

  @Override
  public Boolean doPromise(int proposalId) throws RemoteException {
    return this.acceptor.promise(proposalId);
  }

  @Override
  public Boolean doAccept(int proposalId) throws RemoteException {
    return this.acceptor.accept(proposalId);
  }

  @Override
  public boolean doCommit(KVOperation operation) throws RemoteException {
    return this.learner.learn(operation);
  }

  @Override
  public void reStart(int peerPortNum, String peerHostName) throws RemoteException {
    try {
      KeyValue peer = (KeyValue) Naming.lookup("rmi://" + peerHostName + ":" + peerPortNum + "/KeyValueService");
      dictionary = peer.copyDataStore();
      logger.logInfoMessage("Restart success from server: " + peerPortNum);
    } catch (Exception e) {
      logger.logErrorMessage("Restart failed from server: " + peerPortNum);
    }
  }

  @Override
  public Map<String, String> copyDataStore() {
    Map<String, String> copy = new HashMap<>();
    for(Map.Entry<String, String> kvPair : dictionary.entrySet()) {
      copy.put(kvPair.getKey(), kvPair.getValue());
    }
    return copy;
  }

  // Helper for generate proposal Id for a new proposal/request-operation
  private synchronized int generateProposalId() {
    String s = new SimpleDateFormat("HHmmssSSS").format(new Date());
    return Integer.parseInt(s);
  }


  /**
   * Inner class Proposer - for KeyValue/KeyValueStore(i.e. PAXOS roles for the server)
   */
  public class Proposer extends UnicastRemoteObject implements Serializable {
    private static final long serialVersionUID = 1l;

    protected Proposer() throws RemoteException {
      super();
    }

    public synchronized boolean propose(int proposalId, ICoordinator coordinator) throws RemoteException {
      System.out.println("propose method being called");
      List<KeyValue> acceptors = new ArrayList<>();

      System.out.println("how many number of peers: ");
      System.out.println(coordinator.getServer().size());
      for(Map.Entry<String, Integer> server : coordinator.getServer()) {
        int port = server.getValue();
        String hostName = server.getKey();

        try {
          KeyValue peer = (KeyValue) Naming.lookup("rmi://" + hostName + ":" + port + "/KeyValueService");
          acceptors.add(peer);

        } catch (Exception e) {
          logger.logErrorMessage("Server " + port + " crashed!");
          continue;
        }
      }

      int majority = acceptors.size() / 2 + 1;

      int promisedCount = 0;

      // PHASE 1: PREPARE
      for(KeyValue acceptor : acceptors) {
        try {
          Boolean isPromised = acceptor.doPromise(proposalId);

          if(isPromised == null) {
            logger.logWarningMessage("Prepare: NOT RESPOND proposal " + proposalId + " from Acceptor: " + acceptor.getPortNum());
          } else if(isPromised) {
            promisedCount++;
            logger.logInfoMessage("Prepare: PROMISED proposal " + proposalId + " from Acceptor: " + acceptor.getPortNum());
          } else {
            logger.logInfoMessage("Prepare: REJECTED proposal " + proposalId + " from Acceptor: " + acceptor.getPortNum());
          }
        } catch (Exception e) {
          logger.logWarningMessage("Prepare: NOT RESPOND from Acceptor " + acceptor.getPortNum());
        }
      }

      // not reaching consensus
      if(promisedCount < majority) {
        return false;
      }

      // PHASE 2: ACCEPT
      int acceptedCount = 0;
      for(KeyValue acceptor : acceptors) {
        try {
          Boolean isAccepted = acceptor.doAccept(proposalId);

          if(isAccepted == null) {
            logger.logInfoMessage("Accept: NOT RESPOND proposal " + proposalId + " by Acceptor " + acceptor.getPortNum());
          } else if(isAccepted) {
            acceptedCount++;
            logger.logInfoMessage("Accept: PROMISED proposal " + proposalId + " by Acceptor " + acceptor.getPortNum());
          } else {
            logger.logInfoMessage("Accept: REJECTED proposal " + proposalId + " by Acceptor " + acceptor.getPortNum());
          }
        } catch (Exception e) {
          logger.logWarningMessage("Accept: NOT RESPOND from Acceptor " + acceptor.getPortNum());

        }
      }

      // not reaching consensus
      if(acceptedCount < majority) {
        return false;
      }

      return true;

    }

  }


  /**
   * Inner class Acceptor - for KeyValue/KeyValueStore(i.e. PAXOS roles for the server)
   */
  public class Acceptor extends UnicastRemoteObject implements Serializable {
    private static final long serialVersionUID = 1l;

    protected Acceptor() throws RemoteException {
      super();
    }

    // promise
    public Boolean promise(int proposalId) {

      // Suppose the random failure probability of Acceptor is 10%
      if(Math.random() <= 0.1) {
        logger.logErrorMessage("Random failure occurs at Acceptor!");
        return null;
      }

      ExecutorService executor = Executors.newSingleThreadExecutor();
      FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          if(proposalId <= maxId) {
            // rejected
            return false;
          } else {
            maxId = proposalId;
            return true;
          }
        }
      });

      // future
      try {
        executor.submit(futureTask);

        // timeout for 2 seconds to handle server failures
        return futureTask.get(2, TimeUnit.SECONDS);
      } catch (ExecutionException | InterruptedException | TimeoutException e) {
        logger.logWarningMessage("Server fails.");
        return null;
      }

    }


    // accept
    public Boolean accept(int proposalId) {

      // Suppose the random failure probability of Acceptor is 10%
      if(Math.random() <= 0.1) {
        logger.logErrorMessage("Random failure occurs at Acceptor!");
        return null;
      }

      ExecutorService executor = Executors.newSingleThreadExecutor();
      FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return proposalId == maxId;
        }
      });

      // future
      try {
        executor.submit(futureTask);

        // timeout for 2 seconds to handle server failures
        return futureTask.get(2, TimeUnit.SECONDS);
      } catch (ExecutionException | InterruptedException | TimeoutException e) {
        e.printStackTrace();
        logger.logWarningMessage("Server fails.");
        return null;
      }

    }


  }


  /**
   * Inner class Learner - for KeyValue/KeyValueStore(i.e. PAXOS roles for the server)
   */
  public class Learner implements Serializable {
    private static final long serialVersionUID = 1l;

    // learn and be called by doCommit
    public boolean learn(KVOperation operation) {
      if(operation.getType().equalsIgnoreCase("PUT")) {
        dictionary.put(operation.getKey(), operation.getVal());

        logger.logInfoMessage("REQUEST - PUT; KEY => " + operation.getKey() + "; VALUE => " + operation.getVal());
        logger.logInfoMessage("Response => code: 200;");

        return true;
      } else {
        if(dictionary.containsKey(operation.getKey())) {
          dictionary.remove(operation.getKey());
          logger.logInfoMessage("Response => code: 200; "
                  + "message: Delete operation successful");
          return true;
        } else {
          logger.logWarningMessage("Response => code: 404; "
                  + "message: key not found");
          return false;
        }
      }

    }
  }

}
