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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 * Simple logging class; should be replaced with log4j or the like.
 *
 * @author dirkriehle
 */
public class Log {

    /**
     *
     */
    @Deprecated
    public static StringBuffer logInfo(String logLevel, String logMessage) {
        StringBuffer result = createLogEntry(logLevel);
        addField(result, "message", logMessage);
        return result;
    }

    /**
     *
     */
    @Deprecated
    public static StringBuffer logInfo(String level, String type, String value) {
        StringBuffer result = createLogEntry(level);
        addField(result, type, value);
        return result;
    }

    /**
     *
     */
    @Deprecated
    public static StringBuffer logInfo(String level, String type, String value, String info) {
        StringBuffer result = createLogEntry(level);
        addField(result, type, value);
        addField(result, "info", info);
        return result;
    }

    /**
     *
     */
    @Deprecated
    public static StringBuffer logCreatedObject(String level, String type, String object) {
        StringBuffer result = createLogEntry(level);
        addField(result, "created", type);
        addField(result, "object", object);
        return result;
    }

    /**
     *
     */
    @Deprecated
    public static StringBuffer logError(String l, String s) {
        StringBuffer result = createLogEntry(l);
        addField(result, "error", s);
        return result;
    }

    /**
     *
     */
    protected static final StringBuffer createLogEntry(String level) {
        StringBuffer sb = new StringBuffer(256);

        addField(sb, "level", level);
        addSession(sb);
        return sb;
    }

    /**
     *
     */
    public static final void addField(StringBuffer sb, String name, String value) {
        sb.append(", " + name + "=" + value);
    }

    /**
     *
     */
    public static final void addSession(StringBuffer sb) {
        Session session = SessionManager.getThreadLocalSession();

        String id = (session != null) ? session.getName() : "no-session";
        addField(sb, "session", id);

        String cn = (session != null) ? session.getClientName() : "no-client";
        addField(sb, "client", cn);
    }

    /**
     *
     */
    public static final void addThrowable(StringBuffer sb, Throwable t) {
        addField(sb, "throwable", t.toString());
    }

    /**
     *
     */
    public static final void addStacktrace(StringBuffer sb, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        addField(sb, "stacktrace", sw.toString());
    }

}
