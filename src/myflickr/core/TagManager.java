/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;
import com.flickr4java.flickr.tags.Tag;
import com.flickr4java.flickr.tags.TagRaw;
import java.util.ArrayList;
import java.util.HashSet;
import myflickr.util.MyFlickrLogManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 *
 * @author neil
 */
public class TagManager {
    Flickr flickr;
    HashSet<String> tagSet;
    Logger logger;
    boolean isInit;
    PhotosInterface photosInterface;

    public TagManager(Flickr flickr) {
        this.flickr = flickr;
        this.tagSet = new HashSet<String>();
        this.logger = MyFlickrLogManager.getLogger();
        //isInit = false;
        this.photosInterface = flickr.getPhotosInterface();
    }

    public void initTagManager(String uid) throws FlickrException {
        //if(isInit == true){return;}
        logger.log(Level.INFO, "Invoke TagsInterface.getListUser()...");
        ArrayList<Tag> tagList = (ArrayList<Tag>) flickr.getTagsInterface().getListUser(uid);
        //ArrayList<TagRaw> tagList = (ArrayList<TagRaw>) flickr.getTagsInterface().getListUserRaw(uid);
        //無法取得tag raw, 因為該方法需要使用者登入

        logger.log(Level.INFO, "...Complete");
        if(tagList!=null){
            this.tagSet.clear(); //clear before add
            for(Tag t : tagList){
                this.tagSet.add(t.getValue());
            }
            //for(TagRaw t : tagList){
                //for(String raw : t.getRaw()){
                    //this.tagSet.add(raw);
                //}
            //}
        }
        //isInit = true;
    }

    HashSet<String> getTagCleanSet() {
        return this.tagSet;
    }

    public ArrayList<Photo> SearchPhotoByTags(String[] tags, String uid) throws FlickrException {
        ArrayList<Photo> results = new ArrayList<Photo>();
        SearchParameters para = new SearchParameters();
        para.setUserId(uid);
        para.setTags(tags);
        para.setTagMode("any");  //valid token "all" for AND, "any" for OR
        int offset = 1;
        while(true){
            //System.out.println("Offset = "+ offset);
            logger.log(Level.INFO, "invoke photosInterface.search, count = "+offset);
            PhotoList<Photo> result = photosInterface.search(para, 500, offset);
            //PhotoList<Photo> result = tempFlickr.getPhotosInterface().search(para, 500, offset);
            logger.log(Level.INFO, "invoke photoInterface.search complete");
            //System.out.println("Result size = "+result.size());
            if(result == null || result.size()==0){break;}
            offset++;
            results.addAll(result);
        }
        logger.log(Level.DEBUG, "Found phot size = "+results.size());
        return results;

    }

}
