/*
 * Copyright (c) 2006-2009 by Dirk Riehle, http://dirkriehle.com
 *
 * This file is part of the Wahlzeit photo rating application.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.wahlzeit.services;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An ObjectManager creates/reads/updates/deletes Persistent (objects) from a (relational) Database.
 * It is an abstract superclass that relies an inheritance interface and the Persistent interface.
 * Subclasses for specific types of object need to implement createObject and provide Statements.
 *
 * @author dirkriehle
 */
public abstract class ObjectManager {

    private static final Logger log = Logger.getLogger(ObjectManager.class.getName());
    private static final Key applicationRootKey = KeyFactory.createKey("Application", "Wahlzeit");

    /**
     *  Finds the first Entity with the given key
     */
    protected <E> E readObject(Class<E> type, Long id) {
        log.info("Load Type " + type.toString() + " with ID " + id + " from datastore.");
        return OfyService.ofy().load().type(type).id(id).now();
    }

    /**
     * Reads an Entity of the specified type where the wanted parameter has the given name,
     * e.g. readObject(User.class, "emailAddress", "name@provider.com").
     */
    protected <E> E readObject(Class<E> type, String parameterName, Object parameterValue) {
        log.info("Load Type " + type.toString() + " with parameter " + parameterName + " == " + parameterValue + " from datastore.");
        return OfyService.ofy().load().type(type).ancestor(applicationRootKey).filter(parameterName, parameterValue).first().now();
    }

    /**
     * Reads all Entities of the specified type,
     * e.g. readObject(User.class) to get a list of all users
     */
    protected <E> void readObjects(Collection<E> result, Class<E> type) {
        log.info("Load all Entities of type " + type.toString() + " from datastore.");
        result.addAll(OfyService.ofy().load().type(type).ancestor(applicationRootKey).list());
    }

    /**
     * Reads all Entities of the specified type, where the given property matches the wanted value
     * e.g. readObject(User.class) to get a list of all users
     */
    protected <E> void readObjects(Collection<E> result, Class<E> type, String propertyName, Object value) {
        log.info("Load all Entities of type " + type.toString() + " where parameter " + propertyName + " = " + value.toString() + " from datastore.");
        result.addAll(OfyService.ofy().load().type(type).ancestor(applicationRootKey).filter(propertyName, value).list());
    }

    /**
     * Writes the given Entity to the datastore.
     */
    protected <E> void writeObject(E e) {
        log.log(Level.FINE, "Write Entity  " + e.toString() + " into the datastore.");
        OfyService.ofy().save().entity(e).now();
    }

    /**
     * Updates the given entitiy in the datastore.
     */
    protected <E> void updateObject(E entity) {
        writeObject(entity);
    }

    /**
     * Updates all entities of the given collection in the datastore.
     */
    protected <E>void updateObjects(Collection<E> collection) {
        for(E o : collection) {
            updateObject(o);
        }
    }


    /**
     *
     */
    protected void updateDependents(Persistent obj) {
        // do nothing
        // TODO: check if necessary for photo
    }

    /**
     * Deletes the given entity from the datastore.
     */
    protected <E> void deleteObject(E entity) {
        log.log(Level.FINE, "Delete entity " + entity.toString() + " from datastore.");
        OfyService.ofy().delete().entity(entity).now();
    }

    /**
     * Deletes all entities of the type that have a property with the specified value,
     * e.g. deleteObjects(PhotoCase.class, "wasDecided", true) to delete all cases that have been decided.
     */
    protected <E> void deleteObjects(Class<E> type, String propertyName, Object value) {
        log.log(Level.FINE, "Delete entities of type " + type + " where property " + propertyName + " == " + value.toString() + " from datastore.");
        List<com.googlecode.objectify.Key<E>> keys = OfyService.ofy().load().type(type).ancestor(applicationRootKey).filter(propertyName, value).keys().list();
        OfyService.ofy().delete().type(type).ids(keys);
    }

    /**
     *
     */
    protected void assertIsNonNullArgument(Object arg) {
        assertIsNonNullArgument(arg, "anonymous");
    }

    /**
     *
     */
    protected void assertIsNonNullArgument(Object arg, String label) {
        if (arg == null) {
            throw new IllegalArgumentException(label + " should not be null");
        }
    }

}
