package org.wahlzeit.handlers;

import com.googlecode.objectify.ObjectifyService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.wahlzeit.model.Client;
import org.wahlzeit.model.UserSession;
import org.wahlzeit.services.EmailAddress;
import org.wahlzeit.services.SessionManager;
import org.wahlzeit.webparts.WebPart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lukas Hahmann on 21.05.15.
 */
public class TellFriendTest {

    public UserSessionProvider userSessionProvider = new UserSessionProvider();

    public WebFormHandlerProvider webFormHandlerProvider = new WebFormHandlerProvider();

    public LocalDatastoreServiceTestConfigProvider localDatastoreServiceTestConfigProvider = new LocalDatastoreServiceTestConfigProvider();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(localDatastoreServiceTestConfigProvider)
            .around(userSessionProvider)
            .around(webFormHandlerProvider);

    @ClassRule
    public static SysConfigProvider sysConfigProvider = new SysConfigProvider();

    private UserSession session;
    private WebFormHandler handler;

    @BeforeClass
    public static void setClassUp() {
        ObjectifyService.factory().register(Client.class);
    }

    @Before
    public void setUp() {
        session = (UserSession) SessionManager.getThreadLocalSession();
        handler = webFormHandlerProvider.getWebFormHandler();
    }


    /**
     *
     */
    @Test
    public void testTellFriendMakeWebPart() {
        WebPart part = handler.makeWebPart(session);
        // no failure is good behavior

        EmailAddress to = EmailAddress.getFromString("engel@himmel.de");
        Map<String, String> args = new HashMap<String, String>();
        args.put(TellFriendFormHandler.EMAIL_TO, to.asString());
        String expectedSubject = "Oh well...";
        args.put(TellFriendFormHandler.EMAIL_SUBJECT, expectedSubject);
        handler.handlePost(session, args);

        part = handler.makeWebPart(session);

        String expectedRecipient = to.asString();
        String recipient = part.getValue(TellFriendFormHandler.EMAIL_TO).toString();
        assertTrue("Recipient not as expected, instead: " + recipient, recipient.equals(expectedRecipient));

        String subject = part.getValue(TellFriendFormHandler.EMAIL_SUBJECT).toString();
        assertTrue("Subject not as expected, instead: " + subject, expectedSubject.equals(subject));
    }

    /**
     *
     */
    @Test
    public void testTellFriendPost() {
        EmailAddress from = EmailAddress.getFromString("info@wahlzeit.org");
        EmailAddress to = EmailAddress.getFromString("fan@yahoo.com");
        EmailAddress bcc = session.getConfiguration().getAuditEmailAddress();
        String subject = "Coolest website ever!";
        String body = "You've got to check this out!";

        Map<String, String> args = new HashMap<String, String>();
        args.put(TellFriendFormHandler.EMAIL_FROM, from.asString());
        args.put(TellFriendFormHandler.EMAIL_TO, to.asString());
        args.put(TellFriendFormHandler.EMAIL_SUBJECT, subject);
        args.put(TellFriendFormHandler.EMAIL_BODY, body);

        handler.handlePost(session, args);

        handler.handlePost(session, Collections.EMPTY_MAP); // will fail if email is sent
    }
}
