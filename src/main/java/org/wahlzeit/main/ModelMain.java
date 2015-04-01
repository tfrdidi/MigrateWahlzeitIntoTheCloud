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

package org.wahlzeit.main;

import org.wahlzeit.model.Case;
import org.wahlzeit.model.CaseId;
import org.wahlzeit.model.Globals;
import org.wahlzeit.model.Photo;
import org.wahlzeit.model.PhotoCaseManager;
import org.wahlzeit.model.PhotoFactory;
import org.wahlzeit.model.PhotoId;
import org.wahlzeit.model.PhotoManager;
import org.wahlzeit.model.User;
import org.wahlzeit.model.UserManager;
import org.wahlzeit.services.ConfigDir;
import org.wahlzeit.services.DatabaseConnection;
import org.wahlzeit.services.FileUtil;
import org.wahlzeit.services.OfyService;
import org.wahlzeit.services.SessionManager;
import org.wahlzeit.services.SysConfig;
import org.wahlzeit.services.SysLog;
import org.wahlzeit.servlets.AbstractServlet;

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * A single-threaded Main class with database connection.
 * Can be used by tools that don't want to start a server.
 *
 * @author dirkriehle
 */
public abstract class ModelMain extends AbstractMain {

    private static final Logger log = Logger.getLogger(ModelMain.class.getName());

    /**
     *
     */
    protected void startUp(String rootDir) throws Exception {
        super.startUp(rootDir);

        if (!hasGlobals()) {
            createDefaultGlobals();
        }

        loadGlobals();

        PhotoFactory.initialize();
    }

    /**
     *
     */
    protected boolean hasGlobals() {
        return OfyService.ofy().load().type(Globals.class).filterKey(Globals.DEAULT_ID).first().now() != null;
    }

    /**
     *
     */
    protected void createDefaultGlobals() {
        Globals globals = new Globals();
        globals.setLastUserId(1);
        globals.setLastPhotoId(0);
        globals.setLastCaseId(0);
        globals.setLastSessionId(0);
        OfyService.ofy().save().entity(globals).now();
    }

    /**
     *
     */
    protected void shutDown() throws Exception {
        saveAll();

        super.shutDown();
    }

    /**
     *
     */
    protected void createUser(String userName, String password, String emailAddress, String photoDir) throws Exception {
        UserManager userManager = UserManager.getInstance();
        long confirmationCode = userManager.createConfirmationCode();
        User user = new User(userName, password, emailAddress, confirmationCode);
        userManager.addUser(user);

        PhotoManager photoManager = PhotoManager.getInstance();
        File photoDirFile = new File(photoDir);
        FileFilter photoFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".jpg");
            }
        };

        File[] photoFiles = photoDirFile.listFiles(photoFileFilter);
        for (int i = 0; i < photoFiles.length; i++) {
            Photo newPhoto = photoManager.createPhoto(photoFiles[i]);
            user.addPhoto(newPhoto);
        }
    }


    /**
     *
     */
    public void loadGlobals() throws SQLException {
        Globals globals = OfyService.ofy().load().type(Globals.class).filterKey(Globals.DEAULT_ID).first().now();
        log.info("Load globals  with ID " + Globals.DEAULT_ID + " from datastore.");

        int lastUserId = globals.getLastUserId();
        User.setLastUserId(lastUserId);
        log.info("loaded global variable lastUserId: " + lastUserId);

        int lastPhotoId = globals.getLastPhotoId();
        PhotoId.setCurrentIdFromInt(lastPhotoId);
        log.info("loaded global variable lastPhotoId: " + lastPhotoId);

        int lastCaseId = globals.getLastCaseId();
        Case.setLastCaseId(new CaseId(lastCaseId));
        log.info("loaded global variable lastCaseId: " + lastCaseId);

        int lastSessionId = globals.getLastSessionId();
        AbstractServlet.setLastSessionId(lastSessionId);
        log.info("loaded global variable lastSessionId: " + lastSessionId);
    }

    /**
     *
     */
    public synchronized void saveGlobals() {
        Globals globals = new Globals();

        int lastUserId = User.getLastUserId();
        globals.setLastUserId(lastUserId);
        log.info("saved global variable lastUserId: " + lastUserId);

        int lastPhotoId = PhotoId.getCurrentIdAsInt();
        globals.setLastPhotoId(lastPhotoId);
        log.info("saved global variable lastPhotoId: " + lastPhotoId);

        int lastCaseId = Case.getLastCaseId().asInt();
        globals.setLastCaseId(lastCaseId);
        log.info("saved global variable lastCaseId: " + lastCaseId);

        int lastSessionId = AbstractServlet.getLastSessionId();
        globals.setLastSessionId(lastSessionId);
        log.info("saved global variable lastSessionId: " + lastSessionId);

        OfyService.ofy().save().entity(globals).now();
    }

    /**
     *
     */
    public void saveAll() {
        PhotoCaseManager.getInstance().savePhotoCases();
        PhotoManager.getInstance().savePhotos();
        UserManager.getInstance().saveUsers();

        saveGlobals();
    }

    /**
     *
     */
    protected void runScript(String scriptName) throws SQLException {
        DatabaseConnection dbc = SessionManager.getDatabaseConnection();
        Connection conn = dbc.getRdbmsConnection();

        ConfigDir scriptsDir = SysConfig.getScriptsDir();

        if (scriptsDir.hasDefaultFile(scriptName)) {
            String defaultScriptFileName = scriptsDir.getAbsoluteDefaultConfigFileName(scriptName);
            runScript(conn, defaultScriptFileName);
        }

        if (scriptsDir.hasCustomFile(scriptName)) {
            String customConfigFileName = scriptsDir.getAbsoluteCustomConfigFileName(scriptName);
            runScript(conn, customConfigFileName);
        }
    }

    /**
     *
     */
    protected void runScript(Connection conn, String fullFileName) throws SQLException {
        String query = FileUtil.safelyReadFileAsString(fullFileName);
        SysLog.logQuery(query);

        Statement stmt = conn.createStatement();
        stmt.execute(query);
    }

}
