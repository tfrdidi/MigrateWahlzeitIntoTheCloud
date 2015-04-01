package org.wahlzeit.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import sun.org.mozilla.javascript.tools.shell.Global;

/**
 * Class that combines all global wahlzeit variables that need to be stored
 * to the datastore when the system is restarted.
 *
 * Created by Lukas Hahmann on 01.04.15.
 */
@Entity
public class Globals {

    public static final Long DEAULT_ID = 1L;

    @Id private Long id;

    private int lastPhotoId;
    private int lastUserId;
    private int lastSessionId;
    private int lastCaseId;

    public Globals() {
        id = DEAULT_ID;
    }

    public int getLastUserId() {
        return lastUserId;
    }

    public int getLastPhotoId() {
        return lastPhotoId;
    }

    public int getLastCaseId() {
        return lastCaseId;
    }

    public int getLastSessionId() {
        return lastSessionId;
    }
    public void setLastPhotoId(int lastPhotoId) {
        this.lastPhotoId = lastPhotoId;
    }

    public void setLastUserId(int lastUserId) {
        this.lastUserId = lastUserId;
    }

    public void setLastCaseId(int lastCaseId) {
        this.lastCaseId = lastCaseId;
    }

    public void setLastSessionId(int lastSessionId) {
        this.lastSessionId = lastSessionId;
    }
}
