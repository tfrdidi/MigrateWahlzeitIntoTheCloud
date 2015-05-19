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
 * Logging class for logging system-level messages.
 * System-level log entries are the result of system services and activities.
 *
 * @author dirkriehle
 */
public class SysLog extends Log {

    /**
     *
     */
    public static StringBuffer logSysInfo(String s) {
        return Log.logInfo("sl", s);
    }

    /**
     *
     */
    public static StringBuffer logSysInfo(String type, String value) {
        return Log.logInfo("sl", type, value);
    }

    /**
     *
     */
    public static StringBuffer logSysInfo(String type, String value, String info) {
        return Log.logInfo("sl", type, value, info);
    }

    /**
     *
     */
    public static StringBuffer logCreatedObject(String type, String object) {
        return Log.logCreatedObject("sl", type, object);
    }

    /**
     *
     */
    public static StringBuffer logSysError(String s) {
        return Log.logError("sl", s);
    }

    /**
     *
     */
    public static StringBuffer logThrowable(Throwable t) {
        Throwable cause = t.getCause();
        if (cause != null) {
            logThrowable(cause);
        }

        StringBuffer result = createSysLogEntry();
        addThrowable(result, t);
        addStacktrace(result, t);
        return result;
    }

    /**
     *
     */
    protected static StringBuffer createSysLogEntry() {
        return createLogEntry("sl");
    }

}
