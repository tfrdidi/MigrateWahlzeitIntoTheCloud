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

import org.wahlzeit.model.GlobalsManager;
import org.wahlzeit.model.Photo;
import org.wahlzeit.model.PhotoCaseManager;
import org.wahlzeit.model.PhotoFactory;
import org.wahlzeit.model.PhotoManager;
import org.wahlzeit.model.User;
import org.wahlzeit.model.UserManager;

import java.io.File;
import java.io.FileFilter;

/**
 * A single-threaded Main class with database connection.
 * Can be used by tools that don't want to start a server.
 *
 * @author dirkriehle
 */
public abstract class ModelMain extends AbstractMain {

    /**
     *
     */
    protected void startUp(String rootDir) throws Exception {
        super.startUp(rootDir);

        GlobalsManager.getInstance().loadGlobals();
        UserManager.getInstance().assertAdminExists();

        PhotoFactory.initialize();
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
            //TODO: change to datastore/cloud storage
            //Photo newPhoto = photoManager.createPhoto(photoFiles[i]);
            //user.addPhoto(newPhoto);
        }
    }


    /**
     *
     */
    public void saveAll() {
        PhotoCaseManager.getInstance().savePhotoCases();
        PhotoManager.getInstance().savePhotos();
        UserManager.getInstance().saveUsers();
        GlobalsManager.getInstance().saveGlobals();
    }
}
