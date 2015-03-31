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
import com.googlecode.objectify.annotation.Entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
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
    protected <E> E readObject(Class<E> type, Long key) {
        log.log(Level.FINE, "Load Type " + type.toString() + " with ID " + key + " from datastore.");
        return OfyService.ofy().load().type(type).ancestor(applicationRootKey).filterKey(key).first().now();
    }

    /**
     * Reads an Entity of the specified type where the wanted parameter has the given name,
     * e.g. readObject(User.class, "emailAddress", "name@provider.com").
     */
    protected <E> E readObject(Class<E> type, String parameterName, String parameterValue) {
        log.log(Level.FINE, "Load Type " + type.toString() + " with parameter " + parameterName + " == " + parameterValue  + " from datastore.");
        return OfyService.ofy().load().type(type).ancestor(applicationRootKey).filter(parameterName, parameterValue).first().now();
    }

    /**
     * Reads all Entities of the specified type,
     * e.g. readObject(User.class) to get a list of all users
     */
    protected <E> List<E> readObjects(Class<E> type) {
        log.log(Level.FINE, "Load all Entities of type " + type.toString() + " from datastore.");
        return OfyService.ofy().load().type(type).ancestor(applicationRootKey).list();
    }

    /**
     * Reads all Entities of the specified type, where the given parameter matches the wanted value
     * e.g. readObject(User.class) to get a list of all users
     */
    protected <E> void readObjects(Collection result, Class<E> type, String parameterName, Object value) {
        log.log(Level.FINE, "Load all Entities of type " + type.toString() + " where parameter " + parameterName + " = " + value.toString() + " from datastore.");
        result.addAll(OfyService.ofy().load().type(type).ancestor(applicationRootKey).filter(parameterName, value).list());
    }

    /**
     *
     */
    protected void readObjects(Collection result, PreparedStatement stmt, String value) throws SQLException {
        stmt.setString(1, value);
        SysLog.logQuery(stmt);
        ResultSet rset = stmt.executeQuery();
        while (rset.next()) {
            Persistent obj = createObject(rset);
            result.add(obj);
        }
    }

    /**
     *
     */
    protected abstract Persistent createObject(ResultSet rset) throws SQLException;

    /**
     *
     */
    /*protected void createObject(Persistent obj, PreparedStatement stmt, String value) throws SQLException {
        stmt.setString(1, value);
        SysLog.logQuery(stmt);
        stmt.executeUpdate();
    }*/

    /**
     * Writes the given Entity to the datastore.
     */
    protected <E> void writeObject(E e) {
        log.log(Level.FINE, "Write Entity  " + e.toString() + " into the datastore.");
        OfyService.ofy().save().entity(e).now();
    }

    /**
     *
     */
    protected void createObject(Persistent obj, PreparedStatement stmt, int value) throws SQLException {
        stmt.setInt(1, value);
        SysLog.logQuery(stmt);
        stmt.executeUpdate();
    }

    /**
     *
     */
    protected void updateObject(Persistent obj, PreparedStatement stmt) throws SQLException {
        if (obj.isDirty()) {
            obj.writeId(stmt, 1);
            SysLog.logQuery(stmt);
            ResultSet rset = stmt.executeQuery();
            if (rset.next()) {
                obj.writeOn(rset);
                rset.updateRow();
                updateDependents(obj);
                obj.resetWriteCount();
            } else {
                SysLog.logSysError("trying to update non-existent object: " + obj.getIdAsString() + "(" + obj.toString() + ")");
            }
        }
    }

    /**
     *
     */
    protected void updateObjects(Collection coll, PreparedStatement stmt) throws SQLException {
        for (Iterator i = coll.iterator(); i.hasNext(); ) {
            Persistent obj = (Persistent) i.next();
            updateObject(obj, stmt);
        }
    }

    /**
     *
     */
    protected void updateDependents(Persistent obj) throws SQLException {
        // do nothing
    }

    /**
     *
     */
    protected void deleteObject(Persistent obj, PreparedStatement stmt) throws SQLException {
        obj.writeId(stmt, 1);
        SysLog.logQuery(stmt);
        stmt.executeUpdate();
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
