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

package org.wahlzeit.servlets;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.wahlzeit.handlers.PartUtil;
import org.wahlzeit.handlers.WebFormHandler;
import org.wahlzeit.handlers.WebPageHandler;
import org.wahlzeit.handlers.WebPartHandlerManager;
import org.wahlzeit.model.GcsAdapter;
import org.wahlzeit.model.UserLog;
import org.wahlzeit.model.UserSession;
import org.wahlzeit.services.SysLog;
import org.wahlzeit.webparts.WebPart;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * @author dirkriehle
 */
public class MainServlet extends AbstractServlet {

    /**
     *
     */
    private static final long serialVersionUID = 42L; // any one does; class never serialized

    /**
     *
     */
    public void myGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        UserSession us = ensureUserSession(request);

        String link = request.getRequestURI();
        int linkStart = link.lastIndexOf("/") + 1;
        int linkEnd = link.indexOf(".html");
        if (linkEnd == -1) {
            linkEnd = link.length();
        }

        link = link.substring(linkStart, linkEnd);
        UserLog.logUserInfo("requested", link);

        WebPageHandler handler = WebPartHandlerManager.getWebPageHandler(link);
        String newLink = PartUtil.DEFAULT_PAGE_NAME;
        if (handler != null) {
            Map args = getRequestArgs(request);
            SysLog.logSysInfo("GET arguments: " + getRequestArgsAsString(us, args));
            newLink = handler.handleGet(us, link, args);
        }

        if (newLink.equals(link)) { // no redirect necessary
            WebPart result = handler.makeWebPart(us);
            us.addProcessingTime(System.currentTimeMillis() - startTime);
            configureResponse(us, response, result);
            us.clearSavedArgs(); // saved args go from post to next get
            us.resetProcessingTime();
        } else {
            SysLog.logSysInfo("redirect", newLink);
            redirectRequest(response, newLink);
            us.addProcessingTime(System.currentTimeMillis() - startTime);
        }
    }

    /**
     *
     */
    public void myPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        UserSession us = ensureUserSession(request);

        String link = request.getRequestURI();
        int linkStart = link.lastIndexOf("/") + 1;
        int linkEnd = link.indexOf(".form");
        if (linkEnd != -1) {
            link = link.substring(linkStart, linkEnd);
        } else {
            link = PartUtil.NULL_FORM_NAME;
        }
        UserLog.logUserInfo("postedto", link);

        Map args = getRequestArgs(request);
        SysLog.logSysInfo("POST arguments: " + getRequestArgsAsString(us, args));

        WebFormHandler formHandler = WebPartHandlerManager.getWebFormHandler(link);
        link = PartUtil.DEFAULT_PAGE_NAME;
        if (formHandler != null) {
            link = formHandler.handlePost(us, args);
        }

        redirectRequest(response, link);
        us.addProcessingTime(System.currentTimeMillis() - startTime);
    }

    /**
     *
     */
    protected Map getRequestArgs(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if ((contentType != null) && contentType.startsWith("multipart/form-data")) {
            return getMultiPartRequestArgs(request);
        } else {
            return request.getParameterMap();
        }
    }

    /**
     * Searches for files in the request and puts them in the resulting map with the key "fileName".
     * When a file is found, you can access its path by searching for elements with the key "fileName".
     */
    protected Map getMultiPartRequestArgs(HttpServletRequest request) throws IOException, ServletException {
        Map<String, String> result = new HashMap<String, String>();
        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iterator = upload.getItemIterator(request);

            while (iterator.hasNext()) {
                FileItemStream fileItemStream = iterator.next();
                String filename = fileItemStream.getName();

                if (!fileItemStream.isFormField()) {
                    InputStream inputStream = fileItemStream.openStream();
                    GcsAdapter.getInstance().streamToCloudStorage(inputStream, filename);
                    result.put("fileName", filename);
                }
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        return result;
    }
}
