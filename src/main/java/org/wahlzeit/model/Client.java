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

package org.wahlzeit.model;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import org.wahlzeit.services.EmailAddress;
import org.wahlzeit.services.ObjectManager;

import java.io.Serializable;

/**
 * A Client uses the system. It is an abstract superclass.
 * This package defines guest, user, moderator, and administrator clients.
 *
 * @author dirkriehle
 */
@Entity
public abstract class Client implements Serializable{

    @Id
    protected Long id;

    @Parent
    protected Key parent = ObjectManager.applicationRootKey;

    /**
     *
     */
    @Index
    protected EmailAddress emailAddress = EmailAddress.EMPTY;

    /**
     *
     */
    protected AccessRights rights = AccessRights.NONE;

    /**
     *
     */
    Client() {
        // do nothing
    }

    /**
     * @methodtype initialization
     */
    protected void initialize(AccessRights myRights, EmailAddress myEmailAddress) {
        rights = myRights;
        setEmailAddress(myEmailAddress);
    }

    /**
     * @methodtype get
     */
    public AccessRights getRights() {
        return rights;
    }

    /**
     * @methodtype set
     */
    public void setRights(AccessRights newRights) {
        rights = newRights;
    }

    /**
     * @methodtype boolean-query
     */
    public boolean hasRights(AccessRights otherRights) {
        return AccessRights.hasRights(rights, otherRights);
    }

    /**
     * @methodtype boolean-query
     */
    public boolean hasGuestRights() {
        return hasRights(AccessRights.GUEST);
    }

    /**
     *
     */
    public boolean hasUserRights() {
        return hasRights(AccessRights.USER);
    }

    /**
     * @methodtype boolean-query
     */
    public boolean hasModeratorRights
    () {
        return hasRights(AccessRights.MODERATOR);
    }

    /**
     * @methodtype boolean-query
     */
    public boolean hasAdministratorRights() {
        return hasRights(AccessRights.ADMINISTRATOR);
    }

    /**
     * @methodtype get
     */
    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    /**
     * @methodtype set
     */
    public void setEmailAddress(EmailAddress newEmailAddress) {
        emailAddress = newEmailAddress;
    }

}
