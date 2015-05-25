package org.wahlzeit.handlers;

import org.junit.rules.ExternalResource;
import org.wahlzeit.model.EnglishModelConfig;
import org.wahlzeit.model.Guest;
import org.wahlzeit.model.LanguageConfigs;
import org.wahlzeit.model.ModelConfig;
import org.wahlzeit.model.Photo;
import org.wahlzeit.model.PhotoManager;
import org.wahlzeit.model.UserSession;
import org.wahlzeit.services.ConfigDir;
import org.wahlzeit.services.Language;
import org.wahlzeit.services.SessionManager;
import org.wahlzeit.services.SysConfig;
import org.wahlzeit.utils.StringUtil;
import org.wahlzeit.webparts.WebPartTemplateService;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Rule that provides a <code>UserSession</code> in the <code>SessionManager</code>
 *
 * Created by Lukas Hahmann on 22.05.15.
 */
public class UserSessionProvider extends ExternalResource {

    @Override
    protected void before() throws Throwable {
        HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(UserSession.INITIALIZED)).thenReturn(UserSession.INITIALIZED);
        when(httpSession.getAttribute(UserSession.CONFIGURATION)).thenReturn(new EnglishModelConfig());
        when(httpSession.getAttribute(UserSession.CLIENT)).thenReturn(new Guest());

        Map<String, Object> dummyMap = new HashMap<String, Object>();
        dummyMap.put(UserSession.MESSAGE, "dummy Message");
        when(httpSession.getAttribute(UserSession.SAVED_ARGS)).thenReturn(dummyMap);

        //httpSession.getAttribute(SAVED_ARGS)



        UserSession userSession = new UserSession("testContext", "", httpSession, "de");
        userSession.setConfiguration(LanguageConfigs.get(Language.ENGLISH));
        SessionManager.setThreadLocalSession(userSession);
    }

    @Override
    protected void after() {
        SessionManager.setThreadLocalSession(null);
    }

}
