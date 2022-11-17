package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import coordinator.ICoordinator;
import utils.KVOperation;

/**
 * This Interface represents key-value pair operations applied to the Server.
 * It extends Remote interface to apply RMI.
 */
public interface KeyValue extends Remote {

  /**
   * Insert a Key-Value pair to the storage.
   * Return true if it is agreed by all the peers, otherwise return false.
   *
   * NOTICE: Putting in an already-in-the-store key with a new value will override the old value
   * @param key the unique identifier of the key value pair to be inserted
   * @param value the value of the unique identifier key to be inserted
   * @return true if it is agreed by all the peers, otherwise return false
   * @throws RemoteException
   */
  boolean put(String key, String value) throws RemoteException;

  /**
   * Return the value of the given key, otherwise return null if the given key is not in the store
   * @param key the given key to get
   * @return the value of the given key, otherwise return null if the given key is not in the store
   * @throws RemoteException
   */
  String get(String key) throws RemoteException;

  /**
   * Return the code to specify the delete operation status.
   * For the current version:
   * return 404: key not found
   * return 200: delete successfully
   * return 500: all peers don't agree
   * @param key given key to be deleted
   * @return the corresponding code to specify the delete operation status
   * @throws RemoteException
   */
  int delete(String key) throws RemoteException;

  /**
   * Get the port number of the server.
   * @return the port number of the server
   * @throws RemoteException
   */
  int getPortNum() throws RemoteException;

  /**
   * Get the coordinator of this server being led/connected.
   * NOTICE: currently we only hard-coded one coordinator for easier-testing.
   * @return the coordinator of this server being led/connected to
   * @throws RemoteException
   */
  ICoordinator getCoordinator() throws RemoteException;

  /**
   * Call the promise method in the Acceptor.
   * Acceptor is an inner util class in KeyValue Impl class --- KeyValueStore.
   * Return true for a majority of acceptors agreed to promise, return false for a majority of
   * acceptors don't agree to promise, and otherwise return null for not responding.
   * @param proposalId given proposalId by proposer
   * @return true for a majority of acceptors agreed to promise, return false for a majority of
   * acceptors don't agree to promise, and otherwise return null for not responding
   * @throws RemoteException
   */
  Boolean doPromise(int proposalId) throws RemoteException;

  /**
   * Call the accept method in the Acceptor.
   * Acceptor is an inner util class in KeyValue Impl class --- KeyValueStore.
   * Return true for a majority of acceptors agreed to accept, return false for a majority of
   * acceptors don't agree to accept, and otherwise return null for not responding.
   * @param proposalId given proposalId by proposer
   * @return true for a majority of acceptors agreed to accept, return false for a majority of
   * acceptors don't agree to accept, and otherwise return null for not responding
   * @throws RemoteException
   */
  Boolean doAccept(int proposalId) throws RemoteException;

  /**
   * Call the learn method in the Learner.
   * Learner is an inner util class in KeyValue Impl class --- KeyValueStore.
   * Return true if the commit
   * @param operation the to-be-committed operation
   * @return true if the operation is successful, otherwise return false
   * @throws RemoteException
   */
  boolean doCommit(KVOperation operation) throws RemoteException;

  /**
   * Restart the down server.
   * @param peerPortNum given peer(live) port number
   * @param peerHostName given peer(live) host name
   * @throws RemoteException
   */
  void reStart(int peerPortNum, String peerHostName) throws RemoteException;

  /**
   * Get and Copy the data store from the current live server,
   * for restarting the server by the peer.
   * Being called in reStart().
   * @return the copy data store from the current live server
   * @throws RemoteException
   */
  Map<String, String> copyDataStore() throws RemoteException;

}


