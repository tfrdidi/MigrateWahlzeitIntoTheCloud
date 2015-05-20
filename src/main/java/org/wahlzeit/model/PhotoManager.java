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
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.wahlzeit.agents.AsyncTaskExecutor;
import org.wahlzeit.services.EmailAddress;
import org.wahlzeit.services.ObjectManager;
import org.wahlzeit.services.Persistent;
import org.wahlzeit.services.SysLog;

import java.io.IOException;
import java.util.ArrayList;
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
     * @methodtype init
     * Loads all Photos from the Datastore and holds them in the cache
     */
    public void init() {
        loadPhotos();
    }

    /**
     * @methodtype boolean-query
     * @methodproperty primitive
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
     * @methodtype command
     */
    public void addPhoto(Photo photo) {
        PhotoId id = photo.getId();
        assertIsNewPhoto(id);
        doAddPhoto(photo);

        //AsyncTaskExecutor.savePhotoAsync(id.asString());
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
     * @methodtype command
     */
    public void loadPhotos() {
        Collection<Photo> existingPhotos = ObjectifyService.run(new Work<Collection<Photo>>() {
            @Override
            public Collection<Photo> run() {
                Collection<Photo> existingPhotos = new ArrayList<Photo>();
                readObjects(existingPhotos, Photo.class);
                return existingPhotos;
            }
        });

        log.info(SysLog.logSysInfo("Number of photos found in datastore", String.valueOf(existingPhotos.size())).toString());
        for (Photo photo : existingPhotos) {
            if (!doHasPhoto(photo.getId())) {
                log.config(SysLog.logSysInfo("Load Photo with ID", photo.getIdAsString()).toString());
                loadScaledImages(photo);
                // Todo: load tags
                doAddPhoto(photo);
                try {
                    String ownerName = photo.getOwnerName();
                    log.log(Level.INFO, "Owner of photo {0} is {1}.", new Object[]{photo.getIdAsString(), ownerName});
                    User user = UserManager.getInstance().getUserByName(ownerName);
                    if (user != null) {
                        user.addPhoto(photo);
                        log.info(SysLog.logSysInfo("found user").toString());
                    } else {
                        log.warning(SysLog.logSysInfo("No user found").toString());
                    }
                }
                catch (Exception e) {
                    log.log(Level.WARNING, "problem when loading owner: ", e);
                }
            } else {
                log.config(SysLog.logSysInfo("Already loaded Photo", photo.getIdAsString()).toString());
            }
        }

        log.info(SysLog.logSysInfo("All photos loaded.").toString());
    }


    /**
     *
     */
    public void savePhoto(Photo photo) {
        updateObject(photo);
    }

    @Override
    protected void updateDependents(Persistent obj) {
        if (obj instanceof Photo) {
            Photo photo = (Photo) obj;
            saveScaledImages(photo);
            updateTags(photo);
        }
    }

    /**
     * Removes all tags of the Photo (obj) in the datastore that have been removed by the user
     * and adds all new tags of the photo to the datastore.
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
     * @methodtype command
     * <p/>
     * Writes all Images of the different sizes to Google Cloud Storage.
     */
    protected void saveScaledImages(Photo photo) {
        String photoIdAsString = photo.getId().asString();
        GcsAdapter gcsAdapter = GcsAdapter.getInstance();
        for (PhotoSize photoSize : PhotoSize.values()) {
            Image image = photo.getImage(photoSize);
            if (image != null) {
                try {
                    if (!gcsAdapter.doesImageExist(photoIdAsString, photoSize.asInt())) {
                        gcsAdapter.writeToCloudStorage(image, photoIdAsString, photoSize.asInt());
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Could not store Image in Cloud Storage.", e);
                }
            } else {
                log.info("No photo for size '" + photoSize.asString() + "'");
            }
        }
    }

    /**
     * @methodtype command
     * <p/>
     * Loads all scaled Images of this Photo from Google Cloud Storage
     */
    protected void loadScaledImages(Photo photo) {
        String photoIdAsString = photo.getId().asString();
        GcsAdapter gcsAdapter = GcsAdapter.getInstance();

        for (PhotoSize photoSize : PhotoSize.values()) {
            if (gcsAdapter.doesImageExist(photoIdAsString, photoSize.asInt())) {
                try {
                    Image image = gcsAdapter.readFromCloudStorage(photoIdAsString, photoSize.asInt());
                    photo.setImage(photoSize, image);
                    log.info("Loaded Image of size " + photoSize.asString() + " for Photo " + photoIdAsString);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not load Image of size " + photoSize.asString() + " for Photo " + photoIdAsString);
                }
            } else {
                log.info("PhotoSize " + photoSize.asString() + " does not exist for Photo " + photoIdAsString);
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
        List<PhotoId> result = new LinkedList<PhotoId>();
        int noFilterConditions = filter.getFilterConditions().size();
        log.info(SysLog.logSysInfo("Number of filter conditions", String.valueOf(noFilterConditions)).toString());

        if (noFilterConditions == 0) {
            Collection<PhotoId> candidates = photoCache.keySet();
            int newPhotos = 0;
            for (PhotoId candidate : candidates) {
                if (!filter.processedPhotoIds.contains(candidate)) {
                    result.add(candidate);
                    ++newPhotos;
                }
            }

            log.info(SysLog.logSysInfo("Photos to show", String.valueOf(newPhotos)).toString());
        } else {
            List<Tag> tags = new LinkedList<Tag>();
            for (String condition : filter.getFilterConditions()) {
                readObjects(tags, Tag.class, Tag.TEXT, condition);
            }
            log.info(SysLog.logSysInfo("Number of Tags", String.valueOf(tags.size())).toString());

            // get the list of all photo ids that correspond to the tags
            for (Tag tag : tags) {
                PhotoId photoId = PhotoId.getIdFromString(tag.getPhotoId());
                if (!filter.isProcessedPhotoId(photoId)) {
                    result.add(PhotoId.getIdFromString(tag.getPhotoId()));
                    log.config(SysLog.logSysInfo("Photo ID", tag.getPhotoId(), "Add photo to filter-result.").toString());
                }
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
