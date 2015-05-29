package org.wahlzeit.model;

import org.wahlzeit.services.LogBuilder;
import org.wahlzeit.services.ObjectManager;

import java.util.HashMap;
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
     * Maps names to user
     */
    protected Map<String, Client> clients = new HashMap<String, Client>();


    // add methods -----------------------------------------------------------------------------------------------------

    /**
     * @methodtype set
     * @methodproperty wrapper
     */
    public void addClient(Client client) {
        assertIsNonNullArgument(client);
        assertIsUnknownClientAsIllegalArgument(client);

        doAddClient(client);
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownClientAsIllegalArgument(Client client) {
        if (hasClientByName(client.getName())) {
            throw new IllegalArgumentException(client.getName() + "is already known");
        }
    }

    /**
     * @methodtype set
     * @methodproperty primitive
     */
    protected void doAddClient(Client client) {
        clients.put(client.getName(), client);
        writeObject(client);
        log.config(LogBuilder.createSystemMessage().addParameter("Added new user", client.getName()).toString());
    }


    // get client methods ----------------------------------------------------------------------------------------------

    /**
     * @methodtype boolean query
     */
    public boolean hasClientByName(String name) {
        assertIsNonNullArgument(name, "user-by-name");
        return getClientByName(name) != null;
    }

    /**
     * @methodtype get
     * @methodproperty wrapper
     */
    public Client getClientByName(String name) {
        assertIsNonNullArgument(name, "user name");

        Client result = doGetClientByName(name);

        if (result == null) {
            log.config(LogBuilder.createSystemMessage().addParameter("User not in cache", name).toString());
            result = readObject(Client.class, name);
            if (result != null) {
                doAddClient(result);
            }
        } else {
            log.config(LogBuilder.createSystemMessage().addParameter("User loaded from cache", name).toString());
        }

        return result;
    }


    // has client method -----------------------------------------------------------------------------------------------

    /**
     * @methodtype get
     * @methodproperty primitive
     */
    protected Client doGetClientByName(String name) {
        return clients.get(name);
    }


    // save methods ----------------------------------------------------------------------------------------------------

    /**
     * @methodtype command
     */
    public void saveClients() {
        updateObjects(clients.values());
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
     * @methodproperty wrapper
     */
    public void deleteClient(Client client) {
        assertIsNonNullArgument(client);
        doDeleteClient(client);

        deleteObject(client);

        assertIsUnknownUserAsIllegalState(client);
    }


    // delete methods --------------------------------------------------------------------------------------------------

    /**
     * @methodtype set
     * @methodproperty primtive
     */
    protected void doDeleteClient(Client client) {
        clients.remove(client.getName());
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownUserAsIllegalState(Client client) {
        if (hasClientByName(client.getName())) {
            throw new IllegalStateException(client.getName() + "should not be known");
        }
    }

    /**
     * @methodtype set
     */
    public void removeClient(Client client) {
        saveClient(client);
        clients.remove(client.getName());
    }

    /**
     * @methodtype command
     */
    public void saveClient(Client client) {
        updateObject(client);
    }
}
