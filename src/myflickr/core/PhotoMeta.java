/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

/**
 *
 * @author neil
 */
public class PhotoMeta {
    private String PhotoSetId = Comm.NO_DATA;
    private String PhotoSetTitle = Comm.NOT_IN_PHOTO_SET;
    private ExifDataWrapper exifDataWrapper;

    public PhotoMeta(){
        exifDataWrapper = new ExifDataWrapper();
    }

    /**
     * @return the PhotoSetId
     */
    public String getPhotoSetId() {
        return PhotoSetId;
    }

    /**
     * @param PhotoSetId the PhotoSetId to set
     */
    public void setPhotoSetId(String PhotoSetId) {
        this.PhotoSetId = PhotoSetId;
    }

    /**
     * @return the PhotoSetTitle
     */
    public String getPhotoSetTitle() {
        return PhotoSetTitle;
    }

    /**
     * @param PhotoSetTitle the PhotoSetTitle to set
     */
    public void setPhotoSetTitle(String PhotoSetTitle) {
        this.PhotoSetTitle = PhotoSetTitle;
    }

    /**
     * @return the exifDataWrapper
     */
    public ExifDataWrapper getExifDataWrapper() {
        return exifDataWrapper;
    }

    /**
     * @param exifDataWrapper the exifDataWrapper to set
     */
    public void setExifDataWrapper(ExifDataWrapper exifDataWrapper) {
        this.exifDataWrapper = exifDataWrapper;
    }
}
