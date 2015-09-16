/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.tags.Tag;
/**
 *
 * @author neil
 */
public class PhotoWrapper implements Comparable{
    private Photo photo;
    private PhotoMeta meta;
    private boolean hasExif;
    private int sequenceNo;
    private boolean visiable;
    private StringBuffer strb;

    public PhotoWrapper(){
        this.visiable = true; //default is true
        meta = new PhotoMeta();
    }
    /**
     * @return the photo
     */
    public Photo getPhoto() {
        return photo;
    }

    /**
     * @param photo the photo to set
     */
    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    /**
     * @return the meta
     */
    public PhotoMeta getMeta() {
        return meta;
    }

    /**
     * @param meta the meta to set
     */
    public void setMeta(PhotoMeta meta) {
        this.meta = meta;
    }

    protected void setHasExifData(boolean b) {
        this.hasExif = true;
    }

    public boolean hasExif(){
        return this.hasExif;
    }

    public void setSequenceNumber(int i){
        this.sequenceNo = i;
    }
    public int getSequenceNumber(){
        return this.sequenceNo;
    }

    public int compareTo(Object o){
        Integer mySeq = new Integer(this.sequenceNo);
        PhotoWrapper pw_o = (PhotoWrapper)o;
        return mySeq.compareTo(pw_o.getSequenceNumber());
    }

    /**
     * @return the visiable
     */
    public boolean isVisiable() {
        return visiable;
    }

    /**
     * @param visiable the visiable to set
     */
    public void setVisiable(boolean visiable) {
        //MyFlickrLogManager.getLogger().log(Level.DEBUG, "Set "+visiable+ " to "+this.getPhoto().getTitle());
        this.visiable = visiable;
    }

    public String getTagListToString(){
        if(strb == null){
            strb = new StringBuffer();
            for(Tag t : this.getPhoto().getTags()){
                strb.append(t.getValue()+",");
            }
        }
        return strb.toString();
    }

    @Override
    public String toString(){
        return String.valueOf(this.getSequenceNumber());
    }

}