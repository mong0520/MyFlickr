/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myflickr.core;

import com.flickr4java.flickr.photosets.Photoset;
import java.util.ArrayList;

/**
 *
 * @author Mong
 */
public class PhotoSetWrapper {
    private ArrayList<PhotoWrapper> photos;
    private Photoset pset;
    private boolean isPhotolistGot;
    private boolean createNewPhotoSet;

    
    public PhotoSetWrapper(){
        this.isPhotolistGot = false;
    }
    public PhotoSetWrapper(Photoset ps){
        this.pset = ps;
        this.isPhotolistGot = false;
        this.createNewPhotoSet = false;
    }
    /**
     * @return the photos
     */
    public ArrayList<PhotoWrapper> getPhotos() {
        return photos;
    }

    /**
     * @param photos the photos to set
     */
    public void setPhotos(ArrayList<PhotoWrapper> photos) {
        this.photos = photos;
    }

    /**
     * @return the PhotoSetId
     */
    public Photoset getPhotoSet() {
        return pset;
    }

    /**
     * @param PhotoSetId the PhotoSetId to set
     */
    public void setPhotoSet(Photoset p) {
        this.pset = p;
    }

    public String toString(){
        return this.pset.getTitle();
    }



    public boolean isPhotoListGot() {
        return this.isPhotolistGot;
    }
    public void setPhotoListGot(boolean b){
        this.isPhotolistGot = b;
    }

    public void setCreateNewPhotoSet(boolean CreateNewPhotoSet) {
        this.createNewPhotoSet = CreateNewPhotoSet;
    }
    public boolean getNeedCreateNewPhotoSet(){
        return this.createNewPhotoSet;
    }

}
