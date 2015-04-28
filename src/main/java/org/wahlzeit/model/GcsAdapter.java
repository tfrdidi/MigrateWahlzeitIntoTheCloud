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

import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.logging.Logger;

/**
 * Adapter for the Google Cloud Storage.
 * It offers read and write functions to store Images.
 *
 * Created by Lukas Hahmann on 28.04.15.
 */
public class GcsAdapter {

    private static final Logger log = Logger.getLogger(GcsAdapter.class.getName());

    private static final String BUCKET_NAME = "picturebucket";
    // 2 MB Buffer, does not limit the size of the files
    private static final int BUFFER_LENGTH = 2 * 1024 * 1024;

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
     * @methodtype wrapper
     *
     * Saves the image in the Google Cloud Storage, so you can access it via owner and filename again.
     * An existing file of that owner with that file name is overwritten.
     *
     * @throws InvalidParameterException - one parameter = null or invalid filename
     *
     * @see #readFromCloudStorage(String, String)
     */
    public void writeImageToCloudStorage(Image image, String ownerName, String filename)
            throws InvalidParameterException, IOException {

        assertValidString(ownerName);
        assertValidImage(image);
        assertValidFileName(filename);

        GcsFilename gcsFilename = createGcsFileName(ownerName, filename);
        writeToCloudStorage(image, gcsFilename);
    }

    /**
     * @methodtype wrapper
     *
     * Saves the image in the Google Cloud Storage, so you can access it via ownerName, photoId and size again.
     * An existing file of that owner with that file name is overwritten.
     *
     * @throws InvalidParameterException - one parameter = null or invalid filename
     *
     * @see #readFromCloudStorage(String, String, String)
     */
    public void writeToCloudStorage(Image image, String ownerName, String photoIdAsString, String size)
            throws InvalidParameterException, IOException {

        assertValidString(ownerName);
        assertValidString(photoIdAsString);
        assertValidString(size);
        assertValidImage(image);

        GcsFilename gcsFilename = createGcsFileName(ownerName, photoIdAsString, size);
        writeToCloudStorage(image, gcsFilename);
    }

    /**
     * @methodtype command
     *
     * Writes the file that is read from <code>input</code>into the google cloud storage under the
     * specified <code>fileName</code>.
     *
     * @throws IOException - when can not write to Google Cloud Storage
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
     * @methodtype command
     *
     * Reads an image from Google Cloud Storage via ownerName and filename.
     *
     * @throws IllegalArgumentException - one parameter = null or invalid filename
     * @throws IOException
     *
     * @see #writeImageToCloudStorage(Image, String, String)
     */
    public Image readFromCloudStorage(String ownerName, String filename)
            throws IllegalArgumentException, IOException {

        assertValidString(ownerName);
        assertValidFileName(filename);

        GcsFilename gcsFilename = createGcsFileName(ownerName, filename);
        return readFromCloudStorage(gcsFilename);
    }

    /**
     * @methodtype command
     *
     * Reads an image from Google Cloud Storage via ownerName, photoId and the size.
     *
     * @throws IllegalArgumentException - one parameter = null or emtpy
     * @throws IOException
     *
     * @see #writeToCloudStorage(Image, String, String, String)
     */
    public Image readFromCloudStorage(String ownerName, String photoIdAsString, String size)
            throws IllegalArgumentException, IOException {

        assertValidString(ownerName);
        assertValidString(photoIdAsString);
        assertValidString(size);

        GcsFilename gcsFilename = createGcsFileName(ownerName, photoIdAsString, size);
        return readFromCloudStorage(gcsFilename);
    }

    /**
     * @methodtype command
     *
     * Reads the specified file from Google Cloud Storage. When not found, null is returned.
     *
     * @throws IOException - when can not access Google Cloud Storage, e.g. insufficient rights
     */
    private Image readFromCloudStorage(GcsFilename gcsFilename)
            throws IOException, InvalidParameterException {

        log.info("Read image " + gcsFilename.getObjectName() + " from bucket " + gcsFilename.getBucketName());

        GcsInputChannel readChannel = gcsService.openReadChannel(gcsFilename, 0);
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_LENGTH);
        readChannel.read(bb);
        return ImagesServiceFactory.makeImage(bb.array());
    }



    // methods to create GcsFileNames ---------------------------------------------------------------------------------

    /**
     * @methodtype command
     * <p/>
     * Creates a <code>GcsFilename</code> for the photo in the specified size.
     * The name structure is:
     * <p/>
     * BUCKET_NAME - ownerName/fileName/photoIdAsString
     */
    private GcsFilename createGcsFileName(String owner, String photoIdAsString, String size) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(owner).append("/").append(photoIdAsString).append("/").append(size);
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
    private GcsFilename createGcsFileName(String owner, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(owner).append("/").append(fileName);
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
