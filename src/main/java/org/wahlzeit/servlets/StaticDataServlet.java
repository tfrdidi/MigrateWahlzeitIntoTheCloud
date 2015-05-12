package org.wahlzeit.servlets;

import com.google.appengine.api.images.Image;
import org.wahlzeit.model.GcsAdapter;
import org.wahlzeit.model.Photo;
import org.wahlzeit.model.PhotoManager;
import org.wahlzeit.model.PhotoSize;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet that returns static data like the Photos to the user.
 * <p/>
 * As there are several links for each Photo, this can not the handled via the MainServlet, which has a unique link
 * for each Handler. Instead web.xml redirects all static requests to this Servlet.
 * <p/>
 * Created by Lukas Hahmann on 29.04.15.
 */
public class StaticDataServlet extends AbstractServlet {

    Logger log = Logger.getLogger(StaticDataServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String uri = request.getRequestURI();
            log.info("Handle URI '" + uri + "'");

            if (uri.startsWith(GcsAdapter.PHOTO_FOLDER_PATH_WITH_BUCKET)) {
                String filename = uri.substring(GcsAdapter.PHOTO_FOLDER_PATH_WITH_BUCKET.length());
                Photo photo = PhotoManager.getPhoto(filename.substring(0, filename.length() - 5));
                Image image = null;
                if(photo != null) {
                    // TODO: think about a better solution
                    int size = Integer.valueOf(filename.substring(filename.length() - 5, filename.length() - 4));
                    PhotoSize photoSize = PhotoSize.getFromInt(size);
                    image = photo.getImage(photoSize);
                }
                // if not in cache load from Google Cloud Storage
                if (image == null) {
                    image = GcsAdapter.getInstance().readFromCloudStorage(filename);
                }
                response.getOutputStream().write(image.getImageData());
                response.getOutputStream().flush();
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception geflogen: ", e);
        }
    }
}

