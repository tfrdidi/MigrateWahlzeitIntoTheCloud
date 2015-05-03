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

import com.google.appengine.api.images.Image;
import org.wahlzeit.services.ObjectManager;
import org.wahlzeit.services.Persistent;
import org.wahlzeit.services.SysLog;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
    protected Map<PhotoId, Photo> photoCache = new HashMap<PhotoId, Photo>();

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
        return hasPhoto(PhotoId.getIdFromString(id));
    }

    /**
     *
     */
    public static final boolean hasPhoto(PhotoId id) {
        return getPhoto(id) != null;
    }

    /**
     *
     */
    public static final Photo getPhoto(String id) {
        return getPhoto(PhotoId.getIdFromString(id));
    }

    /**
     *
     */
    public static final Photo getPhoto(PhotoId id) {
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
    protected boolean doHasPhoto(PhotoId id) {
        return photoCache.containsKey(id);
    }

    /**
     *
     */
    public Photo getPhotoFromId(PhotoId id) {
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
    protected Photo doGetPhotoFromId(PhotoId id) {
        return photoCache.get(id);
    }

    /**
     *
     */
    public void addPhoto(Photo photo) {
        PhotoId id = photo.getId();
        assertIsNewPhoto(id);
        doAddPhoto(photo);

        writeObject(photo);
        GlobalsManager.getInstance().saveGlobals();
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

    @Override
    protected void updateDependents(Persistent obj) {
        if(obj instanceof  Photo) {
            Photo photo = (Photo) obj;
            saveScaledImages(photo);
            updateTags(photo);
        }
    }

    /**
     *  Removes all tags of the Photo (obj) in the datastore that have been removed by the user
     *  and adds all new tags of the photo to the datastore.
     */
    protected void updateTags(Photo photo) {
        // delete all existing tags, for the case that some have been removed
        deleteObjects(Tag.class, Tag.PHOTO_ID, photo.getId().asString());

        // add all current tags to the datastore
        Set<String> tags = new HashSet<String>();
        photoTagCollector.collect(tags, photo);
        for (Iterator<String> i = tags.iterator(); i.hasNext(); ) {
            Tag tag = new Tag(i.next(), photo.getId().asString());
            log.info("Write tag: " + tag.asString());
            writeObject(tag);
        }
    }

    /**
     * Writes all Images of the different sizes to Google Cloud Storage.
     */
    protected void saveScaledImages(Photo photo) {
        String photoIdAsString = photo.getId().asString();
        String ending = photo.getEnding();
        for(PhotoSize photoSize : PhotoSize.values()) {
            Image image = photo.getImage(photoSize);
            if(image != null) {
                try {
                    GcsAdapter gcsAdapter = GcsAdapter.getInstance();
                    if(!gcsAdapter.doesImageExist(photoIdAsString, photoSize.asInt(), ending)) {
                        gcsAdapter.writeToCloudStorage(image, photoIdAsString, photoSize.asInt(), ending);
                    }
                }
                catch (Exception e) {
                    log.log(Level.SEVERE, "Could not store image in Cloud Storage.", e);
                }
            }
            else {
                log.info("No photo for size '" + photoSize.asString() + "'");
            }
        }
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
            List<PhotoId> list = getFilteredPhotoIds(filter);
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
                log.info("addProcessedPhoto: " + result.getId().asString());
                filter.addProcessedPhoto(result);
            }
        }

        return result;
    }

    /**
     *
     */
    protected List<PhotoId> getFilteredPhotoIds(PhotoFilter filter) {
        // get all tags that match the filter conditions
        List<Tag> tags = new LinkedList<Tag>();
        int noFilterConditions = filter.getFilterConditions().size();
        log.info("Number of filter conditions: " + noFilterConditions);

        if (noFilterConditions == 0) {
            readObjects(tags, Tag.class);
        }
        else {
            for (String condition : filter.getFilterConditions()) {
                readObjects(tags, Tag.class, Tag.TEXT, condition);
            }
        }

        log.info("Number of tags: " + tags.size());

        // get the list of all photo ids that correspond to the tags
        List<PhotoId> result = new LinkedList<PhotoId>();
        for(Tag tag : tags) {
            PhotoId photoId = PhotoId.getIdFromString(tag.getPhotoId());
            if(!filter.isProcessedPhotoId(photoId)) {
                result.add(PhotoId.getIdFromString(tag.getPhotoId()));
                log.info("Add Photo " + tag.getPhotoId() + " to filter result.");
            }
        }
        return result;
    }

    /**
     *
     */
    public Photo createPhoto(String filename, Image uploadedImage) throws Exception {
        PhotoId id = PhotoId.getNextId();
        Photo result = PhotoUtil.createPhoto(filename, id, uploadedImage);
        addPhoto(result);
        return result;
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsNewPhoto(PhotoId id) {
        if (hasPhoto(id)) {
            throw new IllegalStateException("Photo already exists!");
        }
    }

}
