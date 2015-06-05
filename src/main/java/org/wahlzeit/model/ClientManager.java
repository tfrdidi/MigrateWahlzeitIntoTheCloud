package org.wahlzeit.model;

import org.wahlzeit.services.LogBuilder;
import org.wahlzeit.services.ObjectManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract super class for UserManager. Contains all members and methods that can be offered for all Clients.
 * <p/>
 * Created by Lukas Hahmann on 29.05.15.
 */
public abstract class ClientManager extends ObjectManager {

    private static final Logger log = Logger.getLogger(ClientManager.class.getName());

    /**
     *
     */
    protected static Long lastClientId = 0L;

    /**
     * Maps IDs to user
     */
    protected Map<String, Client> idClientMap = new HashMap<String, Client>();

    protected List<String> listOfUsedNicknames = new ArrayList<String>();


    // add methods -----------------------------------------------------------------------------------------------------

    /**
     * @methodtype set
     * @methodproperty wrapper
     */
    public void addClient(Client client) throws IllegalArgumentException {
        assertIsNonNullArgument(client);
        assertIsUnknownClientAsIllegalArgument(client);
        assertNicknameIsNotUsed(client.getNickName());

        doAddClient(client);
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownClientAsIllegalArgument(Client client) {
        if (hasClientById(client.getId())) {
            throw new IllegalArgumentException(client.getId() + "is already known");
        }
    }

    /**
     * @methodtype assertion
     */
    protected void assertNicknameIsNotUsed(String nickName) {
        if (listOfUsedNicknames.contains(nickName)) {
            throw new IllegalArgumentException("Nickname " + nickName + " is already used.");
        }
    }

    /**
     * @methodtype set
     * @methodproperty primitive
     */
    protected void doAddClient(Client client) {
        idClientMap.put(client.getId(), client);
        writeObject(client);
        listOfUsedNicknames.add(client.getNickName());
        log.config(LogBuilder.createSystemMessage().addParameter("Added new user", client.getId()).toString());
    }


    // get client methods ----------------------------------------------------------------------------------------------

    /**
     * @methodtype boolean query
     */
    public boolean hasClientById(String id) {
        assertIsNonNullArgument(id, "user by Id");
        return getClientById(id) != null;
    }

    /**
     * @methodtype get
     * @methodproperty wrapper
     */
    public Client getClientById(String name) {
        assertIsNonNullArgument(name, "user name");

        Client result = doGetClientById(name);

        return result;
    }


    // has client method -----------------------------------------------------------------------------------------------

    /**
     * @methodtype get
     * @methodproperty primitive
     */
    protected Client doGetClientById(String name) {
        return idClientMap.get(name);
    }


    // save methods ----------------------------------------------------------------------------------------------------

    /**
     * @methodtype command
     */
    public void saveClients() {
        updateObjects(idClientMap.values());
    }

    /**
     * @methodtype get
     */
    public synchronized Long getNextClientId() {
        return ++lastClientId;
    }


    // client ID methods -----------------------------------------------------------------------------------------------

    /**
     * @methodtype get
     */
    public Long getLastClientId() {
        return lastClientId;
    }

    /**
     * @methodtype set
     */
    public synchronized void setLastClientId(Long newId) {
        lastClientId = newId;
    }

    /**
     * @methodtype set
     */
    public void removeClient(Client client) {
        saveClient(client);
        idClientMap.remove(client.getId());
    }


    // delete methods --------------------------------------------------------------------------------------------------

    /**
     * @methodtype command
     */
    public void saveClient(Client client) {
        updateObject(client);
    }

    /**
     * @methodtype set
     * @methodproperty wrapper
     */
    public void deleteClient(Client client) {
        assertIsNonNullArgument(client);
        doDeleteClient(client);

        deleteObject(client);

        assertIsUnknownUserAsIllegalState(client);
    }

    /**
     * @methodtype set
     * @methodproperty primtive
     */
    protected void doDeleteClient(Client client) {
        idClientMap.remove(client.getId());
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownUserAsIllegalState(Client client) {
        if (hasClientById(client.getId())) {
            throw new IllegalStateException(client.getId() + "should not be known");
        }
    }


    // update methods --------------------------------------------------------------------------------------------------

    /**
     * @methodtype set
     */
    public void changeNickname(String oldNickName, String newNickName) throws IllegalArgumentException {
        assertNicknameIsNotUsed(newNickName);

        listOfUsedNicknames.remove(oldNickName);
        listOfUsedNicknames.add(newNickName);
    }
}
