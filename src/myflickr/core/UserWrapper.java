/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.contacts.Contact;
import com.flickr4java.flickr.groups.Group;
import com.flickr4java.flickr.groups.GroupList;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.tags.Tag;
import java.io.Serializable;
import java.util.*;
/**
 *
 * @author neil
 */
public class UserWrapper implements Serializable{
    private String username;
    //private ArrayList<Photo> photoList;
    private ArrayList<PhotoWrapper> photoWrapperList;
    private User user;    
    //private HashMap<String, HashMap<String, String>> photoExifMap;
    private int photosCount;
    private String buddyIconUrl;
    //private HashSet<TagWrapper> tagCloud;
    private HashSet<String> tagSet ;
    private boolean hasExtraData = false;
    private ArrayList<Photoset> photoSetList;
    //有在photoSetListWrapper中的photosetwrapper，都是已經和底下的照片做關聯過的
    private ArrayList<PhotoSetWrapper> photoSetListWrapper;
    private ArrayList<PhotoWrapper> favoritePhotos;
    private ArrayList<Group> groupList;
    private ArrayList<Contact> contactList;

    public UserWrapper(){
        photoWrapperList = new ArrayList<PhotoWrapper>();
        photoSetListWrapper = new ArrayList<PhotoSetWrapper>();
    }

    public UserWrapper(String name){
        this();
        this.username = name;
    }
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

//
//    /**
//     * @return the photoList
//     */
//    public ArrayList<Photo> getPhotoList() {
//        return photoList;
//    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
        this.username = user.getUsername();
//        int iconFarm = user.getIconFarm();
//        int iconServer = user.getIconServer();
//        String nsid = user.getId();
//        http://farm{icon-farm}.staticflickr.com/{icon-server}/buddyicons/{nsid}.jpg
        this.buddyIconUrl = "https://www.flickr.com/buddyicons/"+user.getId()+".jpg";
//        System.out.println(this.buddyIconUrl);
    }

//    /**
//     * @param photoList the photoList to set
//     */
//    public void setPhotoList(ArrayList<Photo> photoList) {
//        this.photoList = photoList;
//    }

    public void setPhotoListEx(ArrayList<PhotoWrapper> photoList) {
        this.photoWrapperList = photoList;
    }

    public void dumpPhotoList(boolean bExif) throws FlickrException{
        if(Comm.DEBUG){
            System.out.println("[Debug] dumpPhotoList:");
        }        
        for(PhotoWrapper pw: photoWrapperList)
        {
            Photo p = pw.getPhoto();
            System.out.println("=====================================================================");
            System.out.println("Photo ID = "+ p.getId());
            System.out.println("Photo Title = "+ p.getTitle());
            System.out.println("Photo URL = "+ p.getUrl());
            System.out.println("Photo Medium800 Url = "+ p.getMedium800Url());
            System.out.println("Photo Medium640 Url = "+ p.getMedium640Url());
            System.out.println("Photo Medium Url = "+ p.getMediumUrl());
            System.out.println("Photo Large Url = "+ p.getLargeUrl());
            System.out.println("Tag Info:");
            ArrayList<Tag> tags = (ArrayList<Tag>) p.getTags();
            for(Tag t : tags){
                System.out.println("Tad ID = " + t.getId());
                System.out.println("Tad Raw = " + t.getRaw());
                System.out.println("Tad Value = " + t.getValue());
            }
            if(p.getOriginalUrl()!=null){
                System.out.println("Photo Original Url = "+ p.getOriginalUrl());
            }
            if(bExif){
                System.out.println("PhotoSet Title = "+ pw.getMeta().getPhotoSetTitle());
                System.out.println("PhotoSet Description = "+ p.getDescription());
                System.out.println("Model = "+ pw.getMeta().getExifDataWrapper().getCameraModel());
                System.out.println("Aperture = "+ pw.getMeta().getExifDataWrapper().getAperture());
                System.out.println("Exposure = "+ pw.getMeta().getExifDataWrapper().getExposure());
                System.out.println("ISO = "+ pw.getMeta().getExifDataWrapper().getIsoSpeed());
                System.out.println("Lens = "+ pw.getMeta().getExifDataWrapper().getLensModel());
//                for(Exif exif : aryExif){
//                    if(exif.getLabel().equalsIgnoreCase("Model")){
//                        System.out.println("[Model] :"+ exif.getRaw());
//                    }
//                    if(exif.getLabel().equalsIgnoreCase("Aperture")){
//                        System.out.println("[Aperture] :"+ exif.getRaw());
//                    }
//                    if(exif.getLabel().equalsIgnoreCase("ISO Speed")){
//                        System.out.println("[ISO Speed] :"+ exif.getRaw());
//                    }
//                    if(exif.getLabel().equalsIgnoreCase("Focal Length")){
//                        System.out.println("[Focal Length] :"+ exif.getRaw());
//                    }
//                    if(exif.getLabel().equalsIgnoreCase("Lens")){
//                        System.out.println("[Lens] :"+ exif.getRaw());
//                    }
//                }
            }
            System.out.println("=====================================================================");
        }
    }

//    /**
//     * @return the photoExifMap
//     */
//    private HashMap<String, HashMap<String, String>> getPhotoExifMap() {
//        return photoExifMap;
//    }
//
//    /**
//     * @param photoExifMap the photoExifMap to set
//     */
//    public void setPhotoExifMap(HashMap<String, HashMap<String, String>> photoExifMap) {
//        this.photoExifMap = photoExifMap;
//    }
//
//    public String getExifData(Photo p, String label){
//        if(p == null || this.getPhotoExifMap() == null || this.getPhotoExifMap().get(p.getId()) == null || this.getPhotoExifMap().get(p.getId()).get(label) == null){
//            return Comm.NO_DATA;
//        }
//        return this.getPhotoExifMap().get(p.getId()).get(label);
//    }


    /**
     * @return the photosCount
     */
    public int getPhotosCount() {
        return photosCount;
    }

    /**
     * @param photosCount the photosCount to set
     */
    public void setPhotosCount(int photosCount) {
        this.photosCount = photosCount;
    }

    /**
     * @return the buddyIconUrl
     */
    public String getBuddyIconUrl() {
        return buddyIconUrl;
    }

    /**
     * @return the photoWrapperList
     */
    public ArrayList<PhotoWrapper> getPhotoWrapperList() {
        return photoWrapperList;
    }

    /**
     * @param photoWrapperList the photoWrapperList to set
     */
    public void setPhotoWrapperList(ArrayList<PhotoWrapper> photoWrapperList) {
        this.photoWrapperList = photoWrapperList;
    }

    public void setHasExtraData(boolean bExtraData) {
        this.hasExtraData = bExtraData;
    }

    public boolean getHasExtraData(){
        return this.hasExtraData;
    }

    public void setTagSet(HashSet<String> tagSet) {
        this.tagSet = tagSet;
    }

    public HashSet<String> getTagSet(){
        return this.tagSet;
    }

    //這裡的photoset只有名稱，並不是wrapper
    public void setPhotoSetList(ArrayList<Photoset> allPhotoSets) {
        this.photoSetList = allPhotoSets;
//        for(Photoset ps : allPhotoSets){
//            PhotoSetWrapper psw = new PhotoSetWrapper(ps);
//        }
    }

    public ArrayList<Photoset> getPhotoSetList(){
        return this.photoSetList;
    }

    /**
     * @return the photoSetListWrapper
     */
    public ArrayList<PhotoSetWrapper> getPhotoSetListEx() {
        return photoSetListWrapper;
    }

    /**
     * @param photoSetListWrapper the photoSetListWrapper to set
     */    
//    public void setPhotoSetListEx(ArrayList<PhotoSetWrapper> photoSetListWrapper) {
//        this.photoSetListWrapper = photoSetListWrapper;
//    }

    public void addPhotoSetWrapper(PhotoSetWrapper psw){
        this.photoSetListWrapper.add(psw);
    }
    
    public boolean isPhotosetExist(Photoset p){
        for(PhotoSetWrapper psw : this.getPhotoSetListEx()){
            Photoset ps = psw.getPhotoSet();
            if(ps.getId().equalsIgnoreCase(p.getId())){
                return true;
            }
        }
        return false;
    }
    
    public ArrayList<PhotoWrapper> getPhotoFromPhotoset(PhotoSetWrapper psw){
        Photoset ps = psw.getPhotoSet();
        if(ps == null){return null;}
        for(PhotoSetWrapper psw_temp : this.getPhotoSetListEx()){
            if(psw_temp.getPhotoSet().getId().equalsIgnoreCase(ps.getId())){
                return psw_temp.getPhotos();
            }
        }        
        return null;
    }

    /**
     * @return the favoritePhotos
     */
    public ArrayList<PhotoWrapper> getFavoritePhotos() {
        return favoritePhotos;
    }

    /**
     * @param favoritePhotos the favoritePhotos to set
     */
    public void setFavoritePhotos(ArrayList<PhotoWrapper> list) {
        this.favoritePhotos = list;
    }

    public void setGroupList(ArrayList<Group> g) {
        this.groupList = g;
    }

    /**
     * @return the groupList
     */
    public ArrayList<Group> getGroupList() {
        return groupList;
    }

    public void setContacts(ArrayList<Contact> contact) {
        this.contactList = contact;
    }

    /**
     * @return the contactList
     */
    public ArrayList<Contact> getContactList() {
        return contactList;
    }
}
