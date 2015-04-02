package org.wahlzeit.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Created by Lukas Hahmann on 01.04.15.
 */
@Entity
public class Tag {

    public static final String TEXT = "text";
    public static final String PHOTO_ID = "photoId";

    @Id private Long id;
    @Index private String text;
    @Index private PhotoId photoId;

    public Tag(String text, PhotoId photoId) {
        this.text = text;
        this.photoId = photoId;
    }

    public String getText() {
        return text;
    }

    public PhotoId getPhotoId() {
        return photoId;
    }
}
