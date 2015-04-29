package org.wahlzeit.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet that returns static data like the Photos to the user.
 *
 * As there are several links for each Photo, this can not the handled via the MainServlet, which has a unique link
 * for each Handler. Instead web.xml redirects all static requests to this Servlet.
 *
 * Created by Lukas Hahmann on 29.04.15.
 */
public class StaticDataServlet extends AbstractServlet {

    @Override
    protected void myGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.warning("Static request: " + request.getRequestURI());
    }
}
