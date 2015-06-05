package org.wahlzeit.model;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import org.wahlzeit.services.SysConfig;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for the Google Cloud Storage. It offers read and write functions to manage Images.
 * <p/>
 * Created by Lukas Hahmann on 28.04.15.
 */
public class GcsAdapter {

    public static final String BUCKET_NAME = SysConfig.DATA_PATH;
    public static final String PHOTO_FOLDER = "photos";
    public static final String PHOTO_FOLDER_PATH_WITH_BUCKET = File.separator + BUCKET_NAME + File.separator + PHOTO_FOLDER + File.separator;
    private static final Logger log = Logger.getLogger(GcsAdapter.class.getName());
    /**
     * 1 MB Buffer, does not limit the size of the files. A valid compromise between unused allocation and
     * reallocation.
     */
    private static final int BUFFER_LENGTH = 1024 * 1024;

    private static final GcsService gcsService =
            GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/jpeg";

    private static GcsAdapter instance = null;

    private GcsAdapter() {

    }

    /**
     * @methodtype get
     */
    public static GcsAdapter getInstance() {
        if (instance == null) {
            instance = new GcsAdapter();
        }
        return instance;
    }


    // write-methods ---------------------------------------------------------------------------------------------------

    /**
     * Saves the image in the Google Cloud Storage, so you can access it via ownerId, photoId and size again. An
     * existing file of that owner with that file name is overwritten.
     *
     * @throws InvalidParameterException - one parameter = null or invalid filename
     * @methodtype command
     * @methodproperty convenience
     * @see #readFromCloudStorage(String, int)
     */
    public void writeToCloudStorage(Image image, String photoIdAsString, int size)
            throws InvalidParameterException, IOException {

        assertValidPhotoId(photoIdAsString);
        PhotoSize.assertIsValidPhotoSizeAsInt(size);
        assertValidImage(image);

        GcsFilename gcsFilename = getGcsFileName(photoIdAsString, size);
        doWriteToCloudStorage(image, gcsFilename);
    }

    /**
     * @methodtype assert
     */
    private void assertValidPhotoId(String photoId) throws IllegalArgumentException {
        if (photoId == null || "".equals(photoId)) {
            throw new IllegalArgumentException("Invalid photoId:" + photoId);
        }
    }


    // read methods ----------------------------------------------------------------------------------------------------

    /**
     * @methodtype assert
     */
    private void assertValidImage(Image image) throws IllegalArgumentException {
        if (image == null) {
            throw new IllegalArgumentException("Invalid image!");
        }
    }

    /**
     * Creates a <code>GcsFilename</code> for the photo in the specified size. The name structure is:
     * <p/>
     * BUCKET_NAME - ownerId/fileName/photoIdAsString
     *
     * @methodtype get
     */
    private GcsFilename getGcsFileName(String photoIdAsString, int size) {
        String filePath = PHOTO_FOLDER + File.separator + photoIdAsString + size;
        return new GcsFilename(BUCKET_NAME, filePath);
    }

    /**
     * Writes the file that is read from <code>input</code>into the google cloud storage under the specified
     * <code>fileName</code>.
     *
     * @throws IOException - when can not write to Google Cloud Storage
     * @methodtype command
     * @methodproperty primitive
     */
    private void doWriteToCloudStorage(Image image, GcsFilename gcsFilename) throws IOException {
        log.log(Level.INFO, "Write image {0} to Datastore {1}.", new Object[]{image, gcsFilename});

        String fileType = URLConnection.guessContentTypeFromName(gcsFilename.getObjectName());

        GcsFileOptions.Builder fileOptionsBuilder = new GcsFileOptions.Builder();
        if (fileType != null) {
            fileOptionsBuilder.mimeType(fileType);
            log.log(Level.CONFIG, "Found file type {0} for {1}.", new Object[]{fileType, gcsFilename.getObjectName()});
        } else {
            fileOptionsBuilder.mimeType(DEFAULT_IMAGE_MIME_TYPE);
            log.log(Level.WARNING, "Did not found a file type for {0}, instead used default: {1}.", new Object[]{gcsFilename.getObjectName(), DEFAULT_IMAGE_MIME_TYPE});
        }

        GcsFileOptions fileOptions = fileOptionsBuilder.build();
        GcsOutputChannel outputChannel =
                gcsService.createOrReplace(gcsFilename, fileOptions);
        outputChannel.write(ByteBuffer.wrap(image.getImageData()));
        outputChannel.close();
    }


    // exist method ----------------------------------------------------------------------------------------------------

    /**
     * Reads an image from Google Cloud Storage via ownerId and filename.
     *
     * @throws IllegalArgumentException - one parameter = null or invalid filename
     * @throws IOException
     * @methodtype get
     * @methodproperty convenience
     * @see #writeToCloudStorage(Image, String, int)
     */
    public Image readFromCloudStorage(String filename)
            throws IllegalArgumentException, IOException {

        assertValidFileName(filename);

        GcsFilename gcsFilename = getGcsFileName(filename);
        return doReadFromCloudStorage(gcsFilename);
    }


    // methods to create GcsFileNames ----------------------------------------------------------------------------------

    /**
     * @methodtype assert
     */
    private void assertValidFileName(String fileName) throws IllegalArgumentException {
        if (fileName == null || "".equals(fileName)) {
            throw new IllegalArgumentException("Invalid file name!");
        }
    }

    /**
     * Creates a <code>GcsFilename</code> for the photo in the specified size. The name structure is:
     * <p/>
     * BUCKET_NAME - ownerId/fileName
     *
     * @methodtype command
     */
    private GcsFilename getGcsFileName(String fileName) {
        String filePath = PHOTO_FOLDER + File.separator + fileName;
        return new GcsFilename(BUCKET_NAME, filePath);
    }


    // assertion methods -----------------------------------------------------------------------------------------------

    /**
     * Reads the specified file from Google Cloud Storage. When not found, null is returned.
     *
     * @throws IOException - when can not access Google Cloud Storage, e.g. insufficient rights
     * @methodtype get
     * @methodproperty primitive
     */
    private Image doReadFromCloudStorage(GcsFilename gcsFilename)
            throws IOException, InvalidParameterException {

        log.log(Level.INFO, "Read image {0} from Clound Storage.", gcsFilename);

        GcsInputChannel readChannel = gcsService.openReadChannel(gcsFilename, 0);
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_LENGTH);
        readChannel.read(bb);
        return ImagesServiceFactory.makeImage(bb.array());
    }

    /**
     * Reads an image from Google Cloud Storage via ownerId, photoId and the size.
     *
     * @throws IOException
     * @throws IllegalArgumentException - one parameter = null or emtpy
     * @methodtype get
     * @methodproperty convenience
     * @see #writeToCloudStorage(Image, String, int)
     */
    public Image readFromCloudStorage(String photoIdAsString, int size)
            throws IllegalArgumentException, IOException {

        assertValidPhotoId(photoIdAsString);
        PhotoSize.assertIsValidPhotoSizeAsInt(size);

        GcsFilename gcsFilename = getGcsFileName(photoIdAsString, size);
        return doReadFromCloudStorage(gcsFilename);
    }

    /**
     * Checks if the specified Image already exists in the Google Cloud Storage
     *
     * @throws IllegalArgumentException
     * @methodtype boolean query
     */
    public boolean doesImageExist(String photoIdAsString, int size)
            throws IllegalArgumentException {

        log.log(Level.CONFIG, "called with PhotoId {0} and size {1}.", new Object[]{photoIdAsString, size});
        assertValidPhotoId(photoIdAsString);
        PhotoSize.assertIsValidPhotoSizeAsInt(size);

        GcsFilename gcsFilename = getGcsFileName(photoIdAsString, size);
        boolean result = false;
        try {
            // will be null if file does not exist
            result = gcsService.getMetadata(gcsFilename) != null;
        } catch (IOException e) {
            // result is already false
        }
        log.log(Level.CONFIG, "Image for PhotoId {0} in size {1} exists: {2}", new Object[]{photoIdAsString, size, result});
        return result;
    }
}
