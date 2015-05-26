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

import org.wahlzeit.model.AccessRights;
import org.wahlzeit.model.User;
import org.wahlzeit.model.UserManager;
import org.wahlzeit.model.UserSession;
import org.wahlzeit.services.EmailAddress;
import org.wahlzeit.services.LogBuilder;
import org.wahlzeit.services.mailing.EmailService;
import org.wahlzeit.services.mailing.EmailServiceManager;
import org.wahlzeit.utils.StringUtil;
import org.wahlzeit.webparts.WebPart;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author dirkriehle
 */
public class EmailPasswordFormHandler extends AbstractWebFormHandler {

    private static final Logger log = Logger.getLogger(EmailPasswordFormHandler.class.getName());

    /**
     *
     */
    public EmailPasswordFormHandler() {
        initialize(PartUtil.EMAIL_PASSWORD_FORM_FILE, AccessRights.GUEST);
    }

    /**
     *
     */
    protected void doMakeWebPart(UserSession us, WebPart part) {
        Map<String, Object> savedArgs = us.getSavedArgs();
        part.addStringFromArgs(savedArgs, UserSession.MESSAGE);
        part.maskAndAddStringFromArgs(savedArgs, User.NAME);
    }

    /**
     *
     */
    protected String doHandlePost(UserSession us, Map args) {
        UserManager userManager = UserManager.getInstance();

        String userName = us.getAndSaveAsString(args, User.NAME);
        if (StringUtil.isNullOrEmptyString(userName)) {
            us.setMessage(us.getConfiguration().getFieldIsMissing());
            return PartUtil.EMAIL_PASSWORD_PAGE_NAME;
        } else if (!userManager.hasUserByName(userName)) {
            us.setMessage(us.getConfiguration().getUserNameIsUnknown());
            return PartUtil.EMAIL_PASSWORD_PAGE_NAME;
        }

        User user = userManager.getUserByName(userName);

        EmailAddress from = us.getConfiguration().getModeratorEmailAddress();
        EmailAddress to = user.getEmailAddress();

        EmailService emailService = EmailServiceManager.getDefaultService();
        emailService.sendEmailIgnoreException(from, to, us.getConfiguration().getAuditEmailAddress(), us.getConfiguration().getSendPasswordEmailSubject(), user.getPassword());

        log.info(LogBuilder.createUserMessage().
                addAction("Password send per E-Mail").
                addParameter("Target address", to.asString()).toString());

        us.setTwoLineMessage(us.getConfiguration().getPasswordWasEmailed(), us.getConfiguration().getContinueWithShowPhoto());

        return PartUtil.SHOW_NOTE_PAGE_NAME;
    }

}
