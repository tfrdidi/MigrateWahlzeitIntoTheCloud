package org.wahlzeit.model;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.ListOptions;
import com.google.appengine.tools.cloudstorage.RetryParams;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.logging.Logger;

/**
 * Adapter for the Google Cloud Storage.
 * It offers read and write functions to store Images.
 * <p/>
 * Created by Lukas Hahmann on 28.04.15.
 */
public class GcsAdapter {

    private static final Logger log = Logger.getLogger(GcsAdapter.class.getName());

    public static final String BUCKET_NAME = "data";
    public static final String PHOTO_FOLDER = "photos";
    public static final String PHOTO_FOLDER_PATH_WITH_BUCKET = File.separator + BUCKET_NAME + File.separator + PHOTO_FOLDER + File.separator;
    public static final String PHOTO_FOLDER_PATH_WITHOUT_BUCKET = PHOTO_FOLDER + File.separator;

    /**
     * 1 MB Buffer, does not limit the size of the files.
     * A valid compromise between unused allocation and reallocation.
     */
    private static final int BUFFER_LENGTH = 1024 * 1024;

    private static final GcsService gcsService =
            GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/jpeg";

    private static GcsAdapter instance = null;

    /**
     * @methodtype get
     */
    public static GcsAdapter getInstance() {
        if (instance == null) {
            instance = new GcsAdapter();
        }
        return instance;
    }

    private GcsAdapter() {

    }


    // write-methods ---------------------------------------------------------------------------------------------------

    /**
     * @throws InvalidParameterException - one parameter = null or invalid filename
     * @methodtype wrapper
     * <p/>
     * Saves the image in the Google Cloud Storage, so you can access it via ownerName, photoId and size again.
     * An existing file of that owner with that file name is overwritten.
     * @see #readFromCloudStorage(String, int, String)
     */
    public void writeToCloudStorage(Image image, String photoIdAsString, int size, String ending)
            throws InvalidParameterException, IOException {

        assertValidString(photoIdAsString);
        assertValidSize(size);
        assertValidImage(image);

        GcsFilename gcsFilename = createGcsFileName(photoIdAsString, size, ending);
        writeToCloudStorage(image, gcsFilename);
    }

    /**
     * @throws IOException - when can not write to Google Cloud Storage
     * @methodtype command
     * <p/>
     * Writes the file that is read from <code>input</code>into the google cloud storage under the
     * specified <code>fileName</code>.
     */
    private void writeToCloudStorage(Image image, GcsFilename gcsFilename) throws IOException {
        log.info("Write file '" + gcsFilename.getObjectName() + "' to cloud storage into the bucket '" + gcsFilename.getBucketName() + "'.");

        String fileType = URLConnection.guessContentTypeFromName(gcsFilename.getObjectName());
        log.info("Found filetype " + fileType + " for " + gcsFilename.getObjectName());

        GcsFileOptions.Builder fileOptionsBuilder = new GcsFileOptions.Builder();
        if (fileType != null) {
            fileOptionsBuilder.mimeType(fileType);
        } else {
            fileOptionsBuilder.mimeType(DEFAULT_IMAGE_MIME_TYPE);
        }

        GcsFileOptions fileOptions = fileOptionsBuilder.build();
        GcsOutputChannel outputChannel =
                gcsService.createOrReplace(gcsFilename, fileOptions);
        outputChannel.write(ByteBuffer.wrap(image.getImageData()));
        outputChannel.close();
    }


    // read methods ----------------------------------------------------------------------------------------------------

    /**
     * @throws IllegalArgumentException - one parameter = null or invalid filename
     * @throws IOException
     * @methodtype command
     * <p/>
     * Reads an image from Google Cloud Storage via ownerName and filename.
     * @see #writeToCloudStorage(Image, String, int, String)
     */
    public Image readFromCloudStorage(String filename)
            throws IllegalArgumentException, IOException {

        assertValidFileName(filename);

        GcsFilename gcsFilename = createGcsFileName(filename);
        return readFromCloudStorage(gcsFilename);
    }

    /**
     * @throws IllegalArgumentException - one parameter = null or emtpy
     * @throws IOException
     * @methodtype command
     * <p/>
     * Reads an image from Google Cloud Storage via ownerName, photoId and the size.
     * @see #writeToCloudStorage(Image, String, int, String)
     */
    public Image readFromCloudStorage(String photoIdAsString, int size, String ending)
            throws IllegalArgumentException, IOException {

        assertValidString(photoIdAsString);
        assertValidSize(size);
        assertValidString(ending);

        GcsFilename gcsFilename = createGcsFileName(photoIdAsString, size, ending);
        return readFromCloudStorage(gcsFilename);
    }

    /**
     * @throws IOException - when can not access Google Cloud Storage, e.g. insufficient rights
     * @methodtype command
     * <p/>
     * Reads the specified file from Google Cloud Storage. When not found, null is returned.
     */
    private Image readFromCloudStorage(GcsFilename gcsFilename)
            throws IOException, InvalidParameterException {

        log.info("Read image " + gcsFilename.getObjectName() + " from bucket " + gcsFilename.getBucketName());

        GcsInputChannel readChannel = gcsService.openReadChannel(gcsFilename, 0);
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_LENGTH);
        readChannel.read(bb);
        return ImagesServiceFactory.makeImage(bb.array());
    }


    // exist method ----------------------------------------------------------------------------------------------------

    /**
     * Checks if the specified Image already exists in the Google Cloud Storage
     *
     * @throws IllegalArgumentException
     */
    public boolean doesImageExist(String photoIdAsString, int size, String ending)
            throws IllegalArgumentException{
        assertValidString(photoIdAsString);
        assertValidSize(size);
        assertValidString(ending);

        GcsFilename gcsFilename = createGcsFileName(photoIdAsString, size, ending);
        try {
            // will be null if file does not exist
            return gcsService.getMetadata(gcsFilename) != null;
        } catch (IOException e) {
            return false;
        }
    }


    // methods to create GcsFileNames ----------------------------------------------------------------------------------

    /**
     * @methodtype command
     * <p/>
     * Creates a <code>GcsFilename</code> for the photo in the specified size.
     * The name structure is:
     * <p/>
     * BUCKET_NAME - ownerName/fileName/photoIdAsString
     */
    private GcsFilename createGcsFileName(String photoIdAsString, int size, String ending) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("photos").append("/").append(photoIdAsString).append(size).append(".").append(ending);
        return new GcsFilename(BUCKET_NAME, stringBuilder.toString());
    }

    /**
     * @methodtype command
     * <p/>
     * Creates a <code>GcsFilename</code> for the photo in the specified size.
     * The name structure is:
     * <p/>
     * BUCKET_NAME - ownerName/fileName
     */
    private GcsFilename createGcsFileName(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("photos").append("/").append(fileName);
        return new GcsFilename(BUCKET_NAME, stringBuilder.toString());
    }


    // assertion methods -----------------------------------------------------------------------------------------------

    /**
     * @methodtype assert
     */
    private void assertValidString(String string) throws IllegalArgumentException {
        if (string == null || string == "") {
            throw new IllegalArgumentException("Invalid owner name!");
        }
    }

    /**
     * @methodtype assert
     */
    private void assertValidSize(int size) throws IllegalArgumentException {
        if(size < 0) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
    }

    /**
     * @methodtype assert
     */
    private void assertValidImage(Image image) throws IllegalArgumentException {
        if (image == null) {
            throw new IllegalArgumentException("Invalid image!");
        }
    }

    /**
     * @methodtype assert
     */
    private void assertValidFileName(String fileName) throws IllegalArgumentException {
        if (fileName == null || "".equals(fileName)) {
            throw new IllegalArgumentException("Invalid file name!");
        } else if (!fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name, name must contain an ending!");
        }
    }
}
