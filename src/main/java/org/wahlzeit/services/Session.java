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


/**
 * A Session object maintains a DatabaseConnection and helps track processing time.
 * Typically, there is one for each working thread, be it a system thread or a web session.
 *
 * @author dirkriehle
 */
public class Session {

    /**
     * Session state
     */
    protected String name = null;


    /**
     * processing time for requests
     */
    protected long processingTime = 0;

    /**
     *
     */
    protected Session() {
        // do nothing
    }

    /**
     *
     */
    protected void initialize(String myName) {
        name = myName;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public String getClientName() {
        return "system";
    }

    /**
     *
     */
    public void resetProcessingTime() {
        processingTime = 0;
    }

    /**
     *
     */
    public void addProcessingTime(long time) {
        processingTime += time;
    }

    /**
     *
     */
    public long getProcessingTime() {
        return processingTime;
    }

}
