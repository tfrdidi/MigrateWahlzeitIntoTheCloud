package org.wahlzeit.services;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * Created by Lukas Hahmann on 31.03.15.
 */
public class Datastore {

    private String applicationName;
    private Key applicationRootKey;

    public Datastore(String applicationName){
        this.applicationName = applicationName;
        this.applicationRootKey = KeyFactory.createKey("Application", applicationName);
    }

    /**
     * Deletes all Entities in the datastore that have the applicationRootKey as parent, grandparent...
     */
    public void deleteDatastore() {
        OfyService.ofy().delete().keys(OfyService.ofy().load().ancestor(applicationRootKey).keys().list()).now();
    }

    public void saveEntity(Entity e) {
        OfyService.ofy().save().entity(e).now();
    }

    public Object loadEntity(Class type, Long id) {
        return OfyService.ofy().load().type(type).filterKey(id).first().now();
    }
}
