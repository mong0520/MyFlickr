/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.favorites.FavoritesInterface;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import java.util.ArrayList;
import myflickr.util.MyFlickrLogManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author neil
 */
public class FavoriteManager {    
    Flickr f;
    FavoritesInterface favoInterface ;
    ArrayList<PhotoWrapper> list;
    private boolean isInit;
    private Logger logger;
    FavoriteManager(Flickr flickr) {
        this.f = flickr;
        favoInterface = f.getFavoritesInterface();
        list = new ArrayList<PhotoWrapper>();
        isInit = false;
        this.logger = MyFlickrLogManager.getLogger();
    }

    public void initFavoriteManager(String uid) throws FlickrException {
        if(f == null){return;}
        int perPage = 500;
        int page = 1;
        while(true){            
            PhotoList<Photo> temp = favoInterface.getPublicList(uid, perPage, page, null);
            if(temp == null || temp.size()==0){break;}
            logger.log(Level.INFO, "Invoke favoInterface.getPublicList()...page = "+page);
            for(Photo p:temp){
                PhotoWrapper pw = new PhotoWrapper();
                pw.setPhoto(p);
                list.add(pw);
            }
            page++;
        }
        isInit = true;
    }

    public ArrayList<PhotoWrapper> getFavoPhotos(){
        return this.list;
    }


    public boolean isInit() {
        return this.isInit;
    }



}
