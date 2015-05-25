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

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.wahlzeit.services.EmailAddress;
import org.wahlzeit.services.ObjectManager;
import org.wahlzeit.services.SysLog;
import org.wahlzeit.services.mailing.EmailService;
import org.wahlzeit.services.mailing.EmailServiceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;


/**
 * The UserManager provides access to and manages Users (including Moderators and Administrators).
 *
 * @author dirkriehle
 */
public class UserManager extends ObjectManager {

    /**
     * Reserved names that cannot be registered by regular users
     *
     * @FIXME Load from file eventually
     */
    public static List<String> reservedNames = Arrays.asList(
            "admin",
            "anonymous",
            "flickr"
    );

    /**
     *
     */
    private UserManager() {
    }

    /**
     *
     */
    protected static UserManager instance = new UserManager();

    private static final Long DEFAULT_ADMIN_ID = 1L;
    private static final Logger log = Logger.getLogger(UserManager.class.getName());

    /**
     *
     */
    public static UserManager getInstance() {
        return instance;
    }

    /**
     * Maps nameAsTag to user of that name (as tag)
     */
    protected Map<String, User> users = new HashMap<String, User>();

    /**
     *
     */
    protected Random codeGenerator = new Random(System.currentTimeMillis());


    public void init() {
        assertAdminExists();
        loadExistingUsers();
    }

    /**
     * @methodtype assert
     */
    public void assertAdminExists() {
        ObjectifyService.run(new Work<Void>() {
            @Override
            public Void run() {
                Collection<Administrator> admins = new ArrayList<Administrator>();
                readObjects(admins, Administrator.class);
                if(admins.size() == 0) {
                    Administrator defaultAdministrator = new Administrator("admin", "admin", "root@localhost", 0);
                    addUser(defaultAdministrator);
                    log.info("No default Administrator exists. Created one.");
                }
                else {
                    log.info("Default Administrator exists.");
                }
                return null;
            }
        });
    }

    /**
     *
     */
    public boolean hasUserByName(String name) {
        assertIsNonNullArgument(name, "user-by-name");
        return hasUserByTag(Tags.asTag(name));
    }

    /**
     *
     */
    public boolean hasUserByTag(String tag) {
        assertIsNonNullArgument(tag, "user-by-tag");
        return getUserByTag(tag) != null;
    }

    /**
     *
     */
    protected boolean doHasUserByTag(String tag) {
        return doGetUserByTag(tag) != null;
    }

    /**
     *
     */
    public User getUserByName(String name) {
        return getUserByTag(Tags.asTag(name));
    }

    /**
     *
     */
    public User getUserByTag(String tag) {
        assertIsNonNullArgument(tag, "user-by-tag");

        User result = doGetUserByTag(tag);

        if (result == null) {
            log.info(SysLog.logSysInfo("User not in cache", tag).toString());
            result = readObject(User.class, User.NAME_AS_TAG, tag);
            if (result != null) {
                doAddUser(result);
            }
        }
        else {
            log.info(SysLog.logSysInfo("User loaded from cache", tag).toString());
        }

        return result;
    }

    /**
     *
     */
    public boolean isReservedUserName(String userName) {
        return reservedNames.contains(Tags.asTag(userName));
    }

    /**
     *
     */
    protected User doGetUserByTag(String tag) {
        return users.get(tag);
    }

    /**
     *
     */
    public void addUser(User user) {
        assertIsNonNullArgument(user);
        assertIsUnknownUserAsIllegalArgument(user);

        writeObject(user);

        doAddUser(user);
    }

    /**
     *
     */
    protected void doAddUser(User user) {
        users.put(user.getNameAsTag(), user);
        log.info("Added new user " + user.getName() + " to the list of all users.");
    }

    /**
     *
     */
    public void deleteUser(User user) {
        assertIsNonNullArgument(user);
        doDeleteUser(user);

        deleteObject(user);

        assertIsUnknownUserAsIllegalState(user);
    }

    /**
     *
     */
    protected void doDeleteUser(User user) {
        users.remove(user.getNameAsTag());
    }

    /**
     *
     */
    public void loadExistingUsers() {
        Collection<User> existingUser = ObjectifyService.run(new Work<Collection<User>>() {
            @Override
            public Collection<User> run() {
                Collection<User> existingUser = new ArrayList<User>();
                readObjects(existingUser, User.class);
                return existingUser;
            }
        });
        log.info(existingUser.size() + " found in the Datastore.");

        for (User user : existingUser) {
            if (!doHasUserByTag(user.getNameAsTag())) {
                doAddUser(user);
            } else {
                log.info("User " + user.getName() + " has already been loaded.");
            }
        }

        log.info("loaded all users");
    }

    /**
     *
     */
    public long createConfirmationCode() {
        return Math.abs(codeGenerator.nextLong() / 2);
    }

    /**
     *
     */
    public void emailWelcomeMessage(UserSession us, User user) {
        EmailAddress from = us.getConfiguration().getAdministratorEmailAddress();
        EmailAddress to = user.getEmailAddress();

        String emailSubject = us.getConfiguration().getWelcomeEmailSubject();
        String emailBody = us.getConfiguration().getWelcomeEmailBody() + "\n\n";
        emailBody += us.getConfiguration().getWelcomeEmailUserName() + user.getName() + "\n\n";
        emailBody += us.getConfiguration().getConfirmAccountEmailBody() + "\n\n";
        emailBody += user.getSiteUrlAsString() + "confirm?code=" + user.getConfirmationCode() + "\n\n"; // @TODO Application
        emailBody += us.getConfiguration().getGeneralEmailRegards() + "\n\n----\n";
        emailBody += us.getConfiguration().getGeneralEmailFooter() + "\n\n";

        EmailService emailService = EmailServiceManager.getDefaultService();
        emailService.sendEmailIgnoreException(from, to, us.getConfiguration().getAuditEmailAddress(), emailSubject, emailBody);
    }

    /**
     *
     */
    public void emailConfirmationRequest(UserSession us, User user) {
        EmailAddress from = us.getConfiguration().getAdministratorEmailAddress();
        EmailAddress to = user.getEmailAddress();

        String emailSubject = us.getConfiguration().getConfirmAccountEmailSubject();
        String emailBody = us.getConfiguration().getConfirmAccountEmailBody() + "\n\n";
        emailBody += user.getSiteUrlAsString() + "confirm?code=" + user.getConfirmationCode() + "\n\n"; // @TODO Application
        emailBody += us.getConfiguration().getGeneralEmailRegards() + "\n\n----\n";
        emailBody += us.getConfiguration().getGeneralEmailFooter() + "\n\n";

        EmailService emailService = EmailServiceManager.getDefaultService();
        emailService.sendEmailIgnoreException(from, to, us.getConfiguration().getAuditEmailAddress(), emailSubject, emailBody);
    }

    /**
     *
     */
    public void saveUser(User user) {
        updateObject(user);
    }

    /**
     *
     */
    public void removeUser(User user) {
        saveUser(user);
        users.remove(user.getNameAsTag());
    }

    /**
     *
     */
    public void saveUsers() {
        updateObjects(users.values());
    }

    /**
     *
     */
    public User getUserByEmailAddress(String emailAddress) {
        return getUserByEmailAddress(EmailAddress.getFromString(emailAddress));
    }

    /**
     *
     */
    public User getUserByEmailAddress(EmailAddress emailAddress) {
        User result = null;
        result = readObject(User.class, User.EMAIL_ADDRESS, emailAddress.asString());

        if (result != null) {
            User current = doGetUserByTag(result.getNameAsTag());
            if (current == null) {
                doAddUser(result);
            } else {
                result = current;
            }
        }

        return result;
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownUserAsIllegalArgument(User user) {
        if (hasUserByTag(user.getNameAsTag())) {
            throw new IllegalArgumentException(user.getName() + "is already known");
        }
    }

    /**
     * @methodtype assertion
     */
    protected void assertIsUnknownUserAsIllegalState(User user) {
        if (hasUserByTag(user.getNameAsTag())) {
            throw new IllegalStateException(user.getName() + "should not be known");
        }
    }

}
