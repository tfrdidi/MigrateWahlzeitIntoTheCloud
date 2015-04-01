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

import org.wahlzeit.main.ServiceMain;
import org.wahlzeit.services.ObjectManager;
import org.wahlzeit.services.OfyService;
import org.wahlzeit.services.Persistent;
import org.wahlzeit.services.SysLog;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A photo manager provides access to and manages photos.
 *
 * @author dirkriehle
 */
public class PhotoManager extends ObjectManager {

    /**
     *
     */
    protected static final PhotoManager instance = new PhotoManager();

    /**
     * In-memory cache for photos
     */
    protected Map<Long, Photo> photoCache = new HashMap<Long, Photo>();

    /**
     *
     */
    protected PhotoTagCollector photoTagCollector = null;

    /**
     *
     */
    public static final PhotoManager getInstance() {
        return instance;
    }

    /**
     *
     */
    public static final boolean hasPhoto(String id) {
        return hasPhoto(Long.valueOf(id));
    }

    /**
     *
     */
    public static final boolean hasPhoto(Long id) {
        return getPhoto(id) != null;
    }

    /**
     *
     */
    public static final Photo getPhoto(String id) {
        return getPhoto(Long.valueOf(id));
    }

    /**
     *
     */
    public static final Photo getPhoto(Long id) {
        return instance.getPhotoFromId(id);
    }

    /**
     *
     */
    public PhotoManager() {
        photoTagCollector = PhotoFactory.getInstance().createPhotoTagCollector();
    }

    /**
     * @methodtype boolean-query
     * @methodproperties primitive
     */
    protected boolean doHasPhoto(Long id) {
        return photoCache.containsKey(id);
    }

    /**
     *
     */
    public Photo getPhotoFromId(Long id) {
        if (id == null) {
            return null;
        }

        Photo result = doGetPhotoFromId(id);

        if (result == null) {
                result = PhotoFactory.getInstance().loadPhoto(id);
            if (result != null) {
                doAddPhoto(result);
            }
        }

        return result;
    }

    /**
     * @methodtype get
     * @methodproperties primitive
     */
    protected Photo doGetPhotoFromId(Long id) {
        return photoCache.get(id);
    }

    /**
     *
     */
    public void addPhoto(Photo photo) {
        Long id = photo.getId();
        assertIsNewPhoto(id);
        doAddPhoto(photo);

        try {
            writeObject(photo);
            ServiceMain.getInstance().saveGlobals();
        } catch (SQLException sex) {
            SysLog.logThrowable(sex);
        }
    }

    /**
     * @methodtype command
     * @methodproperties primitive
     */
    protected void doAddPhoto(Photo myPhoto) {
        photoCache.put(myPhoto.getId(), myPhoto);
    }

    /**
     *
     */
    public void loadPhotos(Collection<Photo> result) {
        readObjects(result, Photo.class);
        for (Iterator<Photo> i = result.iterator(); i.hasNext(); ) {
            Photo photo = i.next();
            if (!doHasPhoto(photo.getId())) {
                doAddPhoto(photo);
            } else {
                SysLog.logSysInfo("photo", String.valueOf(photo.getId()), "photo had already been loaded");
            }
        }

        SysLog.logSysInfo("loaded all photos");
    }

    /**
     *
     */
    public void savePhoto(Photo photo) {
        updateObject(photo);
    }

    /**
     *
     */
    public void savePhotos() {
        updateObjects(photoCache.values());
    }

    /**
     *
     */
    public Set<Photo> findPhotosByOwner(String ownerName) {
        Set<Photo> result = new HashSet<Photo>();
        readObjects(result, Photo.class, Photo.OWNER_NAME, ownerName);

        for (Iterator<Photo> i = result.iterator(); i.hasNext(); ) {
            doAddPhoto(i.next());
        }

        return result;
    }

    /**
     *
     */
    public Photo getVisiblePhoto(PhotoFilter filter) {
        Photo result = getPhotoFromFilter(filter);

        if (result == null) {
            java.util.List<PhotoId> list = getFilteredPhotoIds(filter);
            filter.setDisplayablePhotoIds(list);
            result = getPhotoFromFilter(filter);
        }

        return result;
    }

    /**
     *
     */
    protected Photo getPhotoFromFilter(PhotoFilter filter) {
        PhotoId id = filter.getRandomDisplayablePhotoId();
        Photo result = getPhotoFromId(id);
        while ((result != null) && !result.isVisible()) {
            id = filter.getRandomDisplayablePhotoId();
            result = getPhotoFromId(id);
            if ((result != null) && !result.isVisible()) {
                filter.addProcessedPhoto(result);
            }
        }

        return result;
    }

    /**
     *
     */
    protected java.util.List<PhotoId> getFilteredPhotoIds(PhotoFilter filter) {
        java.util.List<PhotoId> result = new LinkedList<PhotoId>();

        try {
            java.util.List<String> filterConditions = filter.getFilterConditions();

            int noFilterConditions = filterConditions.size();
            PreparedStatement stmt = getUpdatingStatementFromConditions(noFilterConditions);
            for (int i = 0; i < noFilterConditions; i++) {
                stmt.setString(i + 1, filterConditions.get(i));
            }

            SysLog.logQuery(stmt);
            ResultSet rset = stmt.executeQuery();

            if (noFilterConditions == 0) {
                noFilterConditions++;
            }

            int[] ids = new int[PhotoId.getCurrentIdAsInt() + 1];
            while (rset.next()) {
                int id = rset.getInt("photo_id");
                if (++ids[id] == noFilterConditions) {
                    PhotoId photoId = PhotoId.getIdFromInt(id);
                    if (!filter.isProcessedPhotoId(photoId)) {
                        result.add(photoId);
                    }
                }
            }
        } catch (SQLException sex) {
            SysLog.logThrowable(sex);
        }

        return result;
    }

    /**
     *
     */
    protected PreparedStatement getUpdatingStatementFromConditions(int no) throws SQLException {
        String query = "SELECT * FROM tags";
        if (no > 0) {
            query += " WHERE";
        }

        for (int i = 0; i < no; i++) {
            if (i > 0) {
                query += " OR";
            }
            query += " (tag = ?)";
        }

        return getUpdatingStatement(query);
    }

    /**
     *  Removes all tags of the Photo (obj) in the datastore that have been removed by the user
     *  and adds all new tags of the photo to the datastore.
     */
    protected void updateDependents(Persistent obj) throws SQLException {
        Photo photo = (Photo) obj;

        // delete all existing tags, for the case that some have been removed
        deleteObjects(Tag.class, Tag.PHOTO_ID, photo.getId());

        // add all current tags to the datastore
        Set<String> tags = new HashSet<String>();
        photoTagCollector.collect(tags, photo);
        for (Iterator<String> i = tags.iterator(); i.hasNext(); ) {
            Tag tag = new Tag(i.next(), photo.getId());
            writeObject(tag);
        }
    }

    /**
     *
     */
    public Photo createPhoto(File file) throws Exception {
        PhotoId id = PhotoId.getNextId();
        Photo result = PhotoUtil.createPhoto(file, id);
        addPhoto(result);
        return result;
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsNewPhoto(Long id) {
        if (hasPhoto(id)) {
            throw new IllegalStateException("Photo already exists!");
        }
    }

}
