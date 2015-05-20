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

package org.wahlzeit.handlers;

import com.google.appengine.api.images.Image;
import org.wahlzeit.agents.AsyncTaskExecutor;
import org.wahlzeit.model.AccessRights;
import org.wahlzeit.model.Photo;
import org.wahlzeit.model.PhotoManager;
import org.wahlzeit.model.Tags;
import org.wahlzeit.model.User;
import org.wahlzeit.model.UserLog;
import org.wahlzeit.model.UserSession;
import org.wahlzeit.services.SysLog;
import org.wahlzeit.utils.StringUtil;
import org.wahlzeit.webparts.WebPart;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author dirkriehle
 */
public class UploadPhotoFormHandler extends AbstractWebFormHandler {

    private static Logger log = Logger.getLogger(UploadPhotoFormHandler.class.getName());

    /**
     *
     */
    public UploadPhotoFormHandler() {
        initialize(PartUtil.UPLOAD_PHOTO_FORM_FILE, AccessRights.USER);
    }

    /**
     *
     */
    protected void doMakeWebPart(UserSession us, WebPart part) {
        Map<String, Object> args = us.getSavedArgs();
        part.addStringFromArgs(args, UserSession.MESSAGE);

        part.maskAndAddStringFromArgs(args, Photo.TAGS);
    }

    /**
     *
     */
    protected String doHandlePost(UserSession us, Map args) {
        String tags = us.getAndSaveAsString(args, Photo.TAGS);

        if (!StringUtil.isLegalTagsString(tags)) {
            us.setMessage(us.getConfiguration().getInputIsInvalid());
            return PartUtil.UPLOAD_PHOTO_PAGE_NAME;
        }

        try {
            PhotoManager pm = PhotoManager.getInstance();
            String fileName = us.getAsString(args, "fileName");
            Image uploadedImage = us.getUploadedImage();
            Photo photo = pm.createPhoto(fileName, uploadedImage);

            User user = (User) us.getClient();
            user.addPhoto(photo);

            log.info("Tags: " + tags);
            photo.setTags(new Tags(tags));
            log.info("Tags of photo: " + photo.getTags().asString());

            StringBuffer sb = UserLog.createActionEntry("UploadPhoto");
            UserLog.addCreatedObject(sb, "Photo", photo.getId().asString());
            log.info(sb.toString());

            us.setTwoLineMessage(us.getConfiguration().getPhotoUploadSucceeded(), us.getConfiguration().getKeepGoing());
            log.info("Calling asynct task to save ID " + photo.getId().asString());
            AsyncTaskExecutor.savePhotoAsync(photo.getId().asString());
        } catch (Exception ex) {
            SysLog.logThrowable(ex);
            us.setMessage(us.getConfiguration().getPhotoUploadFailed());
        }

        return PartUtil.UPLOAD_PHOTO_PAGE_NAME;
    }
}
