/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

/**
 *
 * @author neil
 */
public class Comm {

    public static enum RetrunCode {
        UNDEF,
        SUCCESS,
        SUCCESS_WITH_ERRORS,
        ERROR,
        FILE_NOT_FOUND,
        NO_PHOTO,
        NO_SUCH_USER,
        PERMISSION_DENIED,
        AUTH_NOT_MATCHED
    }

    public static String NO_DATA = "No data";
    public static String NOT_IN_PHOTO_SET = "未分類";


    public static boolean DEBUG = true;

    public static String EXIF_MODEL = "Model";
    public static String EXIF_EXPOSURE = "Exposure";
    public static String EXIF_APERTURE = "Aperture";
    public static String EXIF_ISOSPEED = "ISO Speed";
    public static String EXIF_LENS = "Lens Model";
    public static String EXIF_LENS_2 = "Lens";
    public static String EXIF_FOCAL_LENGTH = "Focal Length";
    public static String EXIF_DATE_TAKEN = "Date and Time (Original)";

    public static final String LABEL_1 = "1"; //for debug use
    public static final String LABEL_10 = "10";
    public static final String LABEL_50 = "50";
    public static final String LABEL_100 = "100";
    public static final String LABEL_300 = "300";
    public static final String LABEL_500 = "500";
    public static final String LABEL_1000 = "1000";
    public static final String LABEL_1500 = "1500";
    public static final String LABEL_3000 = "3000";
    public static final String LABEL_ALL = "所有照片";

    public static enum PhotoSize {
        Medium_500,
        Medium_640,
        Medium_800,
        Large,
        Large_1600,        
        Original,
        Default,
    }

    public static enum SelectType {
        CAMERA_MODEL,
        LENS_ID,
        FOCAL_LENGTH,
        SHUTTER_SPEED,
        APERUTRE_VALUE,
        ISO_SPEED
    }
    public static enum Tag_Method {
        SET,
        ADD,
        REMOVE
    }

    public static int PROGRESS_TYPE_PHOTO = 0;
    public static int PROGRESS_TYPE_PHOTO_EXIF = 1;
    public static int PROGRESS_TYPE_DOWNLOAD = 2;

    public static String ALREADY_ATUHED = "ALREADY_ATUHED";

    public static String STORAGE_BASE = System.getProperty("user.home")+System.getProperty("file.separator") + "Flickr小幫手";

    public static String CONFIG_FILENAME = "flickrminer.ini";

    public static int PHOTOSET_ACTION_NO_CREATE = 0;
    public static int PHOTOSET_ACTION_CREATE = 1;
}