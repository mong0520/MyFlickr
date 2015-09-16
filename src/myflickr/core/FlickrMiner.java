/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

/**
 *
 * @author neil
 */
import java.net.URISyntaxException;
import org.apache.log4j.*;
import com.flickr4java.flickr.*;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.contacts.Contact;
import com.flickr4java.flickr.contacts.ContactsInterface;
import com.flickr4java.flickr.favorites.FavoritesInterface;
import com.flickr4java.flickr.groups.Group;
import com.flickr4java.flickr.groups.GroupList;
import com.flickr4java.flickr.groups.GroupsInterface;
import com.flickr4java.flickr.groups.pools.PoolsInterface;
import com.flickr4java.flickr.people.*;
import com.flickr4java.flickr.photos.*;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.tags.Tag;
import com.flickr4java.flickr.tags.TagsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.flickr4java.flickr.urls.*;


//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.scribe.model.Token;


import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import myflickr.util.MyFlickrLogManager;
import myflickr.util.SSLTool;
import org.xml.sax.SAXException;


/**
 * A simple program to backup all of a users private and public photos in a photoset aware manner. If photos are classified in multiple photosets, they will be
 * copied. Its a sample, its not perfect :-)
 *
 * This sample also uses the AuthStore interface, so users will only be asked to authorize on the first run.
 *
 * @author Matthew MacKenzie
 * @version $Id: Backup.java,v 1.6 2009/01/01 16:44:57 x-mago Exp $
 */

public class FlickrMiner {

    private final Flickr flickr;
    //private AuthStore authStore;
    //private User user;
    //private Auth auth;
    //private String authLocation;
    private final UserWrapper userWrapper;
    private PeopleInterface peopleInterface ;
    private PhotosInterface photosInterface ;
    private PhotosetsInterface photosetInterface ;
    private ContactsInterface contactInterface;
    private PoolsInterface poolsInterface;
    private TagsInterface tagInterface;
    private UrlsInterface urlInterface;    
    private int currentProcessedPhotoCount;
    private int currentProcessedPhotoExifCount ;
    public int STATUS = -1;
    final static public int STATUS_NOT_START = 0; //0 for nothing, 1 for photo is done, 2 for exif is done
    final static public int STATUS_READY = 1; //0 for nothing, 1 for photo is done, 2 for exif is done
    final static public int STATUS_PHOTO_DONE = 2; //0 for nothing, 1 for photo is done, 2 for exif is done
    final static public int STATUS_PHOTO_AND_EXIF_DONE = 3; //0 for nothing, 1 for photo is done, 2 for exif is done
    final static public int STATUS_EXIF_DONE = 4;
    final static public int STATUS_FINISH = 99; //0 for nothing, 1 for photo is done, 2 for exif is done
    //public FlickrMiner(String apiKey, String username, String sharedSecret, File authsDir) throws FlickrException {
    Token accessToken ;
    Token requestToken;
    //private AuthInterface authInterface;
    //private RequestContext rc;
    private TagManager tagManager;
    private FavoriteManager favManager;
    private AuthManager authManager;
    Uploader uploader ;
    private Logger logger;
    public static int TOTAL_FAIL_COUNT_ON_EXIF_FETCHING = 0; //to support multithread, declare it as static variable, then each thread can calculate the final count easily

    public static void main(String[] args) {
        try {
            //String username = args[0];
            //int outputCount = Integer.parseInt(args[1]);
//            boolean bExtra = true;
//            boolean dumpExif = true;
            //SSLTool.disableCertificateValidation();
            UserWrapper user = new UserWrapper("dio-tw");
            FlickrMiner miner = new FlickrMiner("5d3fd4c39ad5005fb9547fc540647cf9", "83d46646050639ab", user);            
            //miner.initUser();
            //miner.testUpload();
            //miner.buildUserDataEx(1, false, true);            
            Photo p =miner.getPhotoInstance("15390580191");
            Collection<Size> sizes = miner.getPhotosInterface().getSizes("15390580191", true);            
            p.setSizes(sizes);
            for(Size s : sizes){                                
                System.out.println(s.getSource());
            }
            
            //System.out.println(p.getLarge1600Url());           
            //System.out.println(miner.getPhotoInstance("15390580191").getLarge1600Url());
            //miner.setAuthLocation("c:\\temp");
            //miner.authorize();
            //tag status start
//        HashSet<String> tagSet = miner.getTagSetByUser(user.getUser().getId());
//        Iterator<String> it = tagSet.iterator();
//        while(it.hasNext()){
//            System.out.println("Tag Raw = ["+it.next()+"]");
//        }
//        String[] para = {"d700"};
//        ArrayList<Photo> searchResult = miner.SearchPhotoByTags(para, user.getUser().getId());
//        for(Photo p : searchResult){
//            System.out.println("Photo ID = "+p.getId());
//            System.out.println("Photo Title = "+p.getTitle());
//        }
            // tag status end
            //miner.initUser();
            //miner.buildUserData(1,bExtra);
//        Photo p = miner.tagInterface.getListPhoto("8649627454/");
//        ArrayList<Tag> tags = (ArrayList<Tag>) p.getTags();
//        for(Tag t : tags){
//            System.out.println("Tag ID = "+t.getId());
//            System.out.println("Tag Raw = "+t.getRaw());
//            System.out.println("Tag Value = "+t.getValue());
//        }
            //System.out.println(miner.queryPhotoInSet("887744419331"));
            //user.dumpPhotoList(dumpExif);
            //miner.setAuthLocation("c:\\temp");
            //miner.authorize();
            //String[] tags = {"test", "dddddddddd"};
            //miner.addTagsToPhoto("8877419331 ", tags);
            //ArrayList<Photo> allPhoto = user.getPhotoList();
            //HtmlReportGenerator gen = new HtmlReportGenerator();
            //gen.setFileName(user.getUsername());
            //gen.genHtmlByPhotoList(user.getUsername(), allPhoto, true);
            //gen.commitWrite();
//        } catch (IOException ex) {
//            java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (SAXException ex) {
//            java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (FlickrException ex) {
            ex.printStackTrace();
        }

        //tag status start
//        HashSet<String> tagSet = miner.getTagSetByUser(user.getUser().getId());
//        Iterator<String> it = tagSet.iterator();
//        while(it.hasNext()){
//            System.out.println("Tag Raw = ["+it.next()+"]");
//        }

//        String[] para = {"d700"};
//        ArrayList<Photo> searchResult = miner.SearchPhotoByTags(para, user.getUser().getId());
//        for(Photo p : searchResult){
//            System.out.println("Photo ID = "+p.getId());
//            System.out.println("Photo Title = "+p.getTitle());
//        }
        // tag status end

        //miner.initUser();
        //miner.buildUserData(1,bExtra);
//        Photo p = miner.tagInterface.getListPhoto("8649627454/");
//        ArrayList<Tag> tags = (ArrayList<Tag>) p.getTags();
//        for(Tag t : tags){
//            System.out.println("Tag ID = "+t.getId());
//            System.out.println("Tag Raw = "+t.getRaw());
//            System.out.println("Tag Value = "+t.getValue());
//        }        
        //System.out.println(miner.queryPhotoInSet("887744419331"));

        //user.dumpPhotoList(dumpExif);
        //miner.setAuthLocation("c:\\temp");
        //miner.authorize();
        //String[] tags = {"test", "dddddddddd"};
        //miner.addTagsToPhoto("8877419331 ", tags);
        //ArrayList<Photo> allPhoto = user.getPhotoList();
        //HtmlReportGenerator gen = new HtmlReportGenerator();
        //gen.setFileName(user.getUsername());
        //gen.genHtmlByPhotoList(user.getUsername(), allPhoto, true);
        //gen.commitWrite();
    }



    public FlickrMiner(String apiKey, String sharedSecret, UserWrapper userWrapper) throws FlickrException {
        SSLTool.disableCertificateValidation();
        logger = MyFlickrLogManager.getLogger();
        //REST r = new REST();
        //r.setConnectTimeoutMs(10000);
        //r.setReadTimeoutMs(10000);
        this.flickr = new Flickr(apiKey, sharedSecret, new REST());
        //Flickr.debugRequest = true;
        this.photosInterface = flickr.getPhotosInterface();
        this.peopleInterface = flickr.getPeopleInterface();
        this.photosetInterface = flickr.getPhotosetsInterface();
        this.urlInterface = flickr.getUrlsInterface();
        this.userWrapper = userWrapper;        
        this.tagInterface = flickr.getTagsInterface();
        this.contactInterface = flickr.getContactsInterface();
        this.poolsInterface = flickr.getPoolsInterface();
        this.uploader = flickr.getUploader();

        tagManager = new TagManager(flickr);
        favManager = new FavoriteManager(flickr);
        authManager = new AuthManager(this.userWrapper, this.flickr.getAuthInterface());
    }

    public int queryStatus(){
        return this.STATUS;
    }
    public int queryCurrentProcessedPhotoStatus(){
        return currentProcessedPhotoCount;
    }

    public int queryCurrentProcessedExifPhotoStatus(){
        return this.currentProcessedPhotoExifCount;
    }


    public String queryPhotoSetByPhotoId(String pid) throws FlickrException{

        //ArrayList<PhotoPlace> context = (ArrayList<PhotoPlace>) this.photosInterface.getAllContexts(pid);
        PhotoAllContext context = this.getPhotosInterface().getAllContexts(pid);
        if(context != null){
            //return context.get(0).getTitle();
            return context.getPhotoSetList().get(0).getTitle();
        }else{
            return Comm.NO_DATA;
        }
    }

//    public ArrayList<PhotoWrapper> queryPhotoByTag(){
//        return null;
//    }

    public ArrayList<Size> getAvaliableSize(String pid) throws FlickrException{
        /*
            可在這邊加認證過程，用以取得原始size (如果user有鎖)
         */
        ArrayList<Size> arySize = new ArrayList<Size>();
        arySize.addAll(this.getPhotosInterface().getSizes(pid));
        return arySize;
    }
    
    public Comm.RetrunCode _buildPhotoSetList(){
        //logger.log(Level.DEBUG, "Start to process photosets");
        ArrayList<Photoset> allPhotoSets = new ArrayList<Photoset>();
        //HashMap<String, ArrayList<Photo>> PhotoHashMap = new HashMap<String, ArrayList<Photo>>();
        int perPage = 500;
        int offset = 1; //initial value is 1
        try {
            while(true){                
                logger.log(Level.INFO, "Invoke PhotosetInterface.getList()...page = "+offset);
                Photosets sets = this.getPhotosetInterface().getList(this.userWrapper.getUser().getId(),perPage, offset, "");
                //Photosets sets = this.getPhotosetInterface().getList(this.userWrapper.getUser().getId(), perPage, 0);
                logger.log(Level.INFO, "...Complete");
                offset++;
                ArrayList<Photoset> tmpSets = (ArrayList<Photoset>) sets.getPhotosets();
                if(tmpSets == null || tmpSets.size() ==0){break;}
                else{
                    allPhotoSets.addAll(tmpSets);
                }
            }
            for(Photoset ps : allPhotoSets){
                ps.setOwner(this.userWrapper.getUser());  //Must call this to set user id, then later can call ps.getUrl() to get the correct URL with user name
                this.userWrapper.addPhotoSetWrapper(new PhotoSetWrapper(ps));
            }
            this.userWrapper.setPhotoSetList(allPhotoSets);


            //System.out.println(allPhotoSets.size());
            return Comm.RetrunCode.SUCCESS;
            //All photo set
//            for(Photoset ps : allPhotoSets){
//                PhotoList<Photo> photoList =  new PhotoList<Photo>();
//                //add photo list to each photo set
//                System.out.println("Now Processing " + ps.getTitle());
//                offset = 1;
//                while(true){
//                    //System.out.println("Offset = "+ offset);
//                    PhotoList<Photo> bufferPhotoList = this.photosetInterface.getPhotos(ps.getId(), 500, offset++);
//                    photoList.addAll(bufferPhotoList);
//                    if(photoList.size() >= ps.getPhotoCount()){break;}
//                }
//                PhotoHashMap.put(ps.getId(), photoList);
//            }

//
//            if(Comm.DEBUG){
//                System.out.println("Photo Set size = "+ allPhotoSets.size());
//                for(Photoset ps : allPhotoSets){
//                    System.out.println("PhotoSet Title = "+ps.getTitle());
////                    ArrayList<Photo> list = PhotoHashMap.get(ps.getId());
////                    System.out.println("Size = " + list.size());
////                    for(Photo p : list){
////                        System.out.println("- "+p.getTitle());
////                    }
//                }
//            }
            
        } catch (FlickrException ex) {
            //Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
            logger.log(Level.ERROR, ex);
            return Comm.RetrunCode.ERROR;
        }
    }
    
    

//    private Comm.RetrunCode _buildPhotoList(int rangeToList, boolean bExtraData) {
//        User u = this.userWrapper.getUser();
//        this.STATUS = FlickrMiner.STATUS_READY;
//        userWrapper.setHasExtraData(bExtraData);
//        currentProcessedPhotoCount = 0;
//        currentProcessedPhotoExifCount = 0;
//        int offset = 1;
//        int defaultBatchCount = 500; //max = 500, defined by Flickr API
//        int batch = defaultBatchCount;
//        int remaining = rangeToList;
//        if(remaining < defaultBatchCount){
//            remaining = rangeToList;
//        }
//        logger.log(Level.DEBUG, "remaining = " + remaining);
//        //PhotoList<Photo> allPhotoList = new PhotoList<Photo>();
//        ArrayList<PhotoWrapper> allPhotoWrapperList = new ArrayList<PhotoWrapper>();
//        //HashMap<String, HashMap<String, String>> photExifMap = new HashMap<String, HashMap<String, String>>();        
//        logger.log(Level.DEBUG, "Getting Photo list from flickr...");
//        logger.log(Level.DEBUG, "Batch Count = "+ defaultBatchCount + " photots / batch");
//        while(true){
//            try {
//                PhotoList<Photo> tempPhotoList = null;
//                logger.log(Level.INFO, "Invoke PeopleInterface.getPublicPhotos()...page = "+offset);                
//                tempPhotoList = this.peopleInterface.getPublicPhotos(u.getId(), Extras.ALL_EXTRAS, defaultBatchCount, offset);
//                logger.log(Level.INFO, "...complete");
//                if (tempPhotoList.size() == 0) {
//                    logger.log(Level.DEBUG, "No Photo found, break");
//                    break;
//                }
//                offset++;
//                //allPhotoList.addAll(tempPhotoList);
//                for(Photo p: tempPhotoList){
//                    PhotoWrapper pw = new PhotoWrapper();
//                    //logger.log(Level.DEBUG, "Star to call photosInterface.getInfo()");
//                    //pw.setPhoto(this.photosInterface.getInfo(p.getId(), null));  //可能會較慢
//                    pw.setPhoto(p);
//                    allPhotoWrapperList.add(pw);
//                    remaining--;
//                    currentProcessedPhotoCount++;
//                    if(remaining<=0){break;}
//                }                
//                logger.log(Level.DEBUG, "Already processed = " + currentProcessedPhotoCount + "/" + this.userWrapper.getPhotosCount());
//                if (currentProcessedPhotoCount >= rangeToList || remaining <=0) {
//                    //currentProcessedPhotoCount = 0; //reset
//                    //logger.log(Level.DEBUG, "currentProcessedPhotoCount = " + currentProcessedPhotoCount);
//                    //logger.log(Level.DEBUG, "remaining = " + remaining);
//                    break;
//                }
//            } catch (FlickrException ex) {
//                Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
//                this.STATUS = FlickrMiner.STATUS_FINISH;
//                return Comm.RetrunCode.ERROR;
//            }
//            //每做完一個loop就先set一次，可讓之後的UI即時更新
//            if(!allPhotoWrapperList.isEmpty()){
//                this.userWrapper.setPhotoListEx(allPhotoWrapperList);
//            }
//        }
//
//        if(!allPhotoWrapperList.isEmpty()){
//            this.userWrapper.setPhotoListEx(allPhotoWrapperList);
//        }
//        this.STATUS = FlickrMiner.STATUS_PHOTO_DONE;
//        if(Comm.DEBUG){logger.log(Level.DEBUG, "Photo list is build complete");}
//
////        //Build Extra Data
////        if(bExtraData)
////        {
////            if(this.queryExtraPhotoInfo(allPhotoWrapperList)==Comm.RetrunCode.SUCCESS){
////                this.STATUS = FlickrMiner.STATUS_FINISH;
////                return Comm.RetrunCode.SUCCESS;
////            }else{
////                this.STATUS = FlickrMiner.STATUS_FINISH;
////                return Comm.RetrunCode.ERROR;
////            }
////        }
//
//        //finish
//        this.STATUS = FlickrMiner.STATUS_FINISH;
//        //currentProcessedPhotoExifCount = 0;
//        return Comm.RetrunCode.SUCCESS;
//    }
//
//    

private Comm.RetrunCode _buildPhotoListEx(int rangeToList, boolean bExtraData, boolean withPrivate) {
        User u = this.userWrapper.getUser();
        this.STATUS = FlickrMiner.STATUS_READY;
        userWrapper.setHasExtraData(bExtraData);
        currentProcessedPhotoCount = 0;
        currentProcessedPhotoExifCount = 0;
        int offset = 1;
        int defaultBatchCount = 500; //max = 500, defined by Flickr API        
        int remaining = rangeToList;
        if(remaining < defaultBatchCount){
            remaining = rangeToList;
        }
                
        if(withPrivate)
        {
            // Request the premission
            try {
                logger.log(Level.DEBUG, "Start to request the auth");
                if(authManager.authorizeGuiEx(Permission.READ)!=Comm.RetrunCode.SUCCESS){
                    logger.log(Level.DEBUG, "Auth is failed");
                    return Comm.RetrunCode.ERROR;
                }
            } catch (IOException ex) {
                logger.log(Level.ERROR, null, ex);
                return Comm.RetrunCode.ERROR;
            } catch (SAXException ex) {
                logger.log(Level.ERROR, null, ex);
                return Comm.RetrunCode.ERROR;
            } catch (FlickrException ex) {
                logger.log(Level.ERROR, null, ex);
                return Comm.RetrunCode.ERROR;
            } catch (URISyntaxException ex) {
                logger.log(Level.ERROR, null, ex);
                return Comm.RetrunCode.ERROR;
            }
        }
        logger.log(Level.DEBUG, "remaining = " + remaining);
        //PhotoList<Photo> allPhotoList = new PhotoList<Photo>();
        ArrayList<PhotoWrapper> allPhotoWrapperList = new ArrayList<PhotoWrapper>();
        //HashMap<String, HashMap<String, String>> photExifMap = new HashMap<String, HashMap<String, String>>();        
        logger.log(Level.DEBUG, "Getting Photo list from flickr...");
        logger.log(Level.DEBUG, "Batch Count = "+ defaultBatchCount + " photots / batch");
        while(true){
            try {
                PhotoList<Photo> tempPhotoList = null;                                
                if(withPrivate){
                    logger.log(Level.INFO, "Invoke PeopleInterface.getPhotos()...page = "+offset);                
                    tempPhotoList = this.peopleInterface.getPhotos(u.getId(), null, null, null, null, null, null, null, Extras.ALL_EXTRAS, defaultBatchCount, offset);
                }else{
                    logger.log(Level.INFO, "Invoke PeopleInterface.getPublicPhotos()...page = "+offset);      
                    tempPhotoList = this.peopleInterface.getPublicPhotos(u.getId(), Extras.ALL_EXTRAS, defaultBatchCount, offset);
//                p.setSizes(sizes);
                }
                logger.log(Level.INFO, "...complete");
                if (tempPhotoList.size() == 0) {
                    logger.log(Level.DEBUG, "No Photo found, break");
                    break;
                }
                offset++;
                //allPhotoList.addAll(tempPhotoList);                
                for(final Photo p: tempPhotoList){
                    PhotoWrapper pw = new PhotoWrapper();
                    //logger.log(Level.DEBUG, "Star to call photosInterface.getInfo()");
                    //pw.setPhoto(this.photosInterface.getInfo(p.getId(), null));  //可能會較慢
                    //logger.log(Level.DEBUG, "Add Photo "+p.getTitle()+" in to PhotoWrapper list");                   
                    pw.setPhoto(p);
                    allPhotoWrapperList.add(pw);
                    remaining--;
                    currentProcessedPhotoCount++;
                    if(remaining<=0){break;}
                }                
                logger.log(Level.DEBUG, "Already processed = " + currentProcessedPhotoCount + "/" + this.userWrapper.getPhotosCount());
                if (currentProcessedPhotoCount >= rangeToList || remaining <=0) {
                    //currentProcessedPhotoCount = 0; //reset
                    //logger.log(Level.DEBUG, "currentProcessedPhotoCount = " + currentProcessedPhotoCount);
                    //logger.log(Level.DEBUG, "remaining = " + remaining);
                    break;
                }
            } catch (FlickrException ex) {
                Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
                this.STATUS = FlickrMiner.STATUS_FINISH;
                return Comm.RetrunCode.ERROR;
            }
            //每做完一個loop就先set一次，可讓之後的UI即時更新
            if(!allPhotoWrapperList.isEmpty()){
                this.userWrapper.setPhotoListEx(allPhotoWrapperList);
            }
        }

        if(!allPhotoWrapperList.isEmpty()){
            this.userWrapper.setPhotoListEx(allPhotoWrapperList);
        }
        this.STATUS = FlickrMiner.STATUS_PHOTO_DONE;
        if(Comm.DEBUG){logger.log(Level.DEBUG, "Photo list is build complete");}

//        //Build Extra Data
//        if(bExtraData)
//        {
//            if(this.queryExtraPhotoInfo(allPhotoWrapperList)==Comm.RetrunCode.SUCCESS){
//                this.STATUS = FlickrMiner.STATUS_FINISH;
//                return Comm.RetrunCode.SUCCESS;
//            }else{
//                this.STATUS = FlickrMiner.STATUS_FINISH;
//                return Comm.RetrunCode.ERROR;
//            }
//        }

        //finish
        this.STATUS = FlickrMiner.STATUS_FINISH;
        //currentProcessedPhotoExifCount = 0;
        return Comm.RetrunCode.SUCCESS;
    }
    

    

//    private boolean isAlreadyAuthorized(){
//        try {
//            this.authStore = new FileAuthStore(new File(this.authLocation));
//            if (this.authStore != null) {
//                logger.log(Level.DEBUG, "Get permission information from path "+ this.authLocation);
//                auth = this.authStore.retrieve(this.userWrapper.getUser().getId());
//                if (auth == null) {
//                    logger.log(Level.DEBUG, "not yet grant the permission");
//                    return false;
//                } else {
//                    if(auth.getPermission().getType() == Permission.DELETE_TYPE){logger.log(Level.DEBUG, "Current Permission =  Delete");}
//                    else if(auth.getPermission().getType() == Permission.READ_TYPE){logger.log(Level.DEBUG, "Current Permission =  Read");}
//                    else if(auth.getPermission().getType() == Permission.WRITE_TYPE){logger.log(Level.DEBUG, "Current Permission =  Write");}
//                    else if(auth.getPermission().getType() == Permission.NONE_TYPE){logger.log(Level.DEBUG, "Current Permission =  None");}
//                    else{logger.log(Level.DEBUG, "Current Permission = "+auth.getPermission().toString());}
//                    return true;
//                }
//            }
//            return false;
//        } catch (FlickrException ex) {
//            Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
//            return false;
//        }
//    }

//    public Comm.RetrunCode buildUserData(int rangeToList, boolean buildExif)  {
//        try {
//            Comm.RetrunCode rt_1 = Comm.RetrunCode.UNDEF;
//            //Comm.RetrunCode rt_2 = Comm.RetrunCode.UNDEF;
//            rt_1 = this._buildPhotoList(rangeToList, buildExif);
//            //rt_2 = this._buildPhotoSetList();
//            if(rt_1 == Comm.RetrunCode.SUCCESS ){
//                return Comm.RetrunCode.SUCCESS;
//            }else{
//                return Comm.RetrunCode.ERROR;
//            }
//
//        } catch (FlickrRuntimeException ex){
//            logger.log(Level.ERROR, null, ex);
//            ex.printStackTrace();
//            System.out.println("Runtime error");
//            return Comm.RetrunCode.ERROR;
//        }
//        catch (Exception ex) {
//            logger.log(Level.ERROR, null, ex);
//            return Comm.RetrunCode.ERROR;
//        }
//    }
    
    public Comm.RetrunCode buildUserDataEx(int rangeToList, boolean buildExif, boolean withPrivate)  {
        try {
            Comm.RetrunCode rt_1 = Comm.RetrunCode.UNDEF;
            //Comm.RetrunCode rt_2 = Comm.RetrunCode.UNDEF;
            rt_1 = this._buildPhotoListEx(rangeToList, buildExif, withPrivate);            
            //rt_2 = this._buildPhotoSetList();
            if(rt_1 == Comm.RetrunCode.SUCCESS ){
                return Comm.RetrunCode.SUCCESS;
            }else{
                return Comm.RetrunCode.ERROR;
            }

        } catch (FlickrRuntimeException ex){
            logger.log(Level.ERROR, null, ex);
            ex.printStackTrace();
            System.out.println("Runtime error");
            return Comm.RetrunCode.ERROR;
        }
        catch (Exception ex) {
            logger.log(Level.ERROR, null, ex);
            return Comm.RetrunCode.ERROR;
        }
    }

    //Get the OAuth request token - this is step one of authorization.
   

//backup
    //test fucntion in command line, do not use it in GUI
//    public void authorize() throws IOException, SAXException, FlickrException {
//        rc = RequestContext.getRequestContext();
//        //check if already authorized.
//        if (isAlreadyAuthorized()){
//            rc.setAuth(auth);
//        }else{
//            if (this.authLocation != null) {
//                this.authStore = new FileAuthStore(new File(this.authLocation));
//            }
//
//            authInterface = flickr.getAuthInterface();
//            requestToken = authInterface.getRequestToken();
//            String url = authInterface.getAuthorizationUrl(accessToken, Permission.WRITE);
//            System.out.println("Follow this URL to authorise yourself on Flickr");
//            System.out.println(url);
//            System.out.println("Paste in the token it gives you:");
//            System.out.print(">>");
//
//            String tokenKey = new Scanner(System.in).nextLine();
//            accessToken = authInterface.getAccessToken(accessToken, new Verifier(tokenKey));
//            auth = authInterface.checkToken(accessToken);
//            rc.setAuth(auth);
//            this.authStore.store(auth);
//            System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.");
//        }
//    }

    

    public Comm.RetrunCode initUser() throws FlickrException {
        String username = "";
        String userId = "";
        User user = null;

        //get user name
        logger.log(Level.INFO, "Invoke UrlInterface.lookupUser...User Input = "+ userWrapper.getUsername());
        username = this.urlInterface.lookupUser("https://www.flickr.com/photos/"+userWrapper.getUsername());
        logger.log(Level.INFO, "...Complete, UserName = [" + username +"]");
        //get user id
        logger.log(Level.INFO, "Invoke peopleInterface.findByUsername...Username = "+ username);
        userId = this.peopleInterface.findByUsername(username).getId();
        logger.log(Level.INFO, "...Complete");
        //get user instance
        logger.log(Level.INFO, "Invoke PeopleInterface.getInfo()...");
        user = this.peopleInterface.getInfo(userId);
        this.userWrapper.setUser(user);
        this.userWrapper.setPhotosCount(user.getPhotosCount());
        logger.log(Level.INFO, "...Complete");

        logger.log(Level.INFO, "Build users PhotoSet list starts");
        if(this._buildPhotoSetList() != Comm.RetrunCode.SUCCESS){
            return Comm.RetrunCode.ERROR;
        }
        logger.log(Level.INFO, "Build users PhotoSet list ends");

        logger.log(Level.INFO, "getTagSetByUser() starts");
        this.userWrapper.setTagSet(this.getTagSetByUser(user.getId()));
        logger.log(Level.INFO, "getTagSetByUser() ends");

        logger.log(Level.INFO, "getFavoritePhotoByUser() starts");
        this.userWrapper.setFavoritePhotos(this.getFavoritePhotoByUser(user.getId()));
        logger.log(Level.INFO, "getFavoritePhotoByUser() ends");

        logger.log(Level.INFO, "getContactByUser() starts");
        this.userWrapper.setContacts(this.getContactByUser(user.getId()));
        logger.log(Level.INFO, "getContactByUser() ends");

        
        logger.log(Level.INFO, "getGroupByUser() starts");
        this.userWrapper.setGroupList(this.getGroupListByUser(user.getId()));
        logger.log(Level.INFO, "getGroupByUser() ends");
        

//            ArrayList<Tag> tags = (ArrayList<Tag>) this.tagInterface.getListUser(user.getId());
//            for(Tag t : tags){
//                //logger.log(Level.DEBUG, "Tag ID = "+t.getId());
//                //logger.log(Level.DEBUG, "Tag Value = "+t.getValue());  //only value could be get
//                //logger.log(Level.DEBUG, "Tag Raw = "+t.getRaw());
//            }
        //userWrapper.setTagList(tags);
        
            logger.log(Level.DEBUG, "User Name = "+ user.getUsername());
            logger.log(Level.DEBUG, "User ID = "+ user.getId());
            logger.log(Level.DEBUG, "User Photo Count = "+userWrapper.getPhotosCount());
            //logger.log(Level.DEBUG, "User Buddy Icon = "+ userWrapper.getBuddyIconUrl());
            //logger.log(Level.DEBUG, "Icon Farm = "+ user.getIconFarm());
            //logger.log(Level.DEBUG, "Icon Server = "+ user.getIconServer());

        return Comm.RetrunCode.SUCCESS;
    }

    public Comm.RetrunCode setTagsToPhoto(String pid, String[] tags) throws FlickrException {
        logger.log(Level.DEBUG, "Invoke PhotosInterface.setTags(), Photo = ["+pid+"]");
        this.getPhotosInterface().setTags(pid, tags);
        return Comm.RetrunCode.SUCCESS;
    }

    public Comm.RetrunCode addTagsToPhoto(String pid, String[] tags) throws FlickrException {
        logger.log(Level.DEBUG, "Invoke PhotosInterface.addTags(), Photo = ["+pid+"]");
        getPhotosInterface().addTags(pid, tags);
        return Comm.RetrunCode.SUCCESS;
    }

public ArrayList<Tag> getTagsByPhoto(String pid) {
        try {
            logger.log(Level.DEBUG, "Invoke TagInterface.getListPhoto with Photo ["+pid+"]");
            Photo p = this.tagInterface.getListPhoto(pid);
            logger.log(Level.DEBUG, "Invoke TagInterface.getListPhoto Complete");
            ArrayList<Tag> tagList = (ArrayList<Tag>) p.getTags();
            return tagList;
        } catch (FlickrException ex) {
            logger.log(Level.DEBUG, "Invoke TagInterface.getListPhoto Error");
            Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
            return null;
        }
    }




    public Photo getPhotoInstance(String pid) {
        try {
            return this.getPhotosInterface().getPhoto(pid);
        } catch (FlickrException ex) {
            Logger.getLogger(FlickrMiner.class.getName()).log(Level.ERROR, null, ex);
            return null;
        }
    }

    public ArrayList<PhotoWrapper> getPhotosByGruop(String gid) throws FlickrException{
        ArrayList<PhotoWrapper> result = new ArrayList<PhotoWrapper>();
        logger.info("Invoke poolsInterface.getPhotos starts");
        PhotoList<Photo> list = poolsInterface.getPhotos(gid, null, 500, 0);
        logger.info("Invoke poolsInterface.getPhotos ends");
        for(Photo p:list){
            PhotoWrapper pw = new PhotoWrapper();
            pw.setPhoto(p);
            result.add(pw);
        }
        return result;
    }

    //dev function, need to get auth first
//    public HashSet<String> getRawTagSet(){
//        try {
//            this.tagManager.initTagManager();
//            return this.tagManager.getTagRawSet();
//        } catch (FlickrException ex) {
//            Logger.getLogger(FlickrMiner.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        }
//
//    }

    public HashSet<String> getTagSetByUser(String uid) throws FlickrException{
        //取得的是tag value, 不是tag raw
        //例如11-22 的raw是"11-22", value是"1122"        
        logger.log(Level.INFO, "initTagManager() starts");
        this.getTagManager().initTagManager(uid);
        logger.log(Level.INFO, "initTagManager() ends");
        return this.getTagManager().getTagCleanSet();
    }

    public ArrayList<PhotoWrapper> getFavoritePhotoByUser(String uid) throws FlickrException{
        if(this.favManager.isInit()){
            logger.info("favManager is already init, return photo list directly");
            return this.favManager.getFavoPhotos();
        }else{
            logger.info("initFavoriteManager() starts");
            this.favManager.initFavoriteManager(uid);
            logger.info("initFavoriteManager() ends");
            return this.favManager.getFavoPhotos();
        }
    }

    public ArrayList<Group> getGroupListByUser(String uid) throws FlickrException{
        ArrayList<Group> gList = (ArrayList<Group>) this.peopleInterface.getPublicGroups(uid);
        return gList;
    }

    public ArrayList<Contact> getContactByUser(String uid) throws FlickrException{
        //ArrayList<Contact> list =
        Collection<Contact> c = this.contactInterface.getPublicList(uid);
        ArrayList<Contact> list = new ArrayList<Contact>(c);
        return list;

    }
    public String queryPathAliasName(String uid) throws FlickrException{
        String fullUrl = this.urlInterface.getUserPhotos(uid);
        String prefix = "https://www.flickr.com/photos/";
        return fullUrl.substring(prefix.length(), fullUrl.length()-1);
    }

    public ArrayList<Photo> SearchPhotoByTags(String[] tags, String uid) throws FlickrException {
//        ArrayList<Photo> results = new ArrayList<Photo>();
//        SearchParameters para = new SearchParameters();
//        para.setUserId(uid);
//        para.setTags(tags);
//        para.setTagMode("any");  //valid token "all" for AND, "any" for OR
//        int offset = 1;
//        while(true){
//            //System.out.println("Offset = "+ offset);
//            logger.log(Level.INFO, "invoke photosInterface.search, count = "+offset);
//            PhotoList<Photo> result = this.photosInterface.search(para, 500, offset);
//            //PhotoList<Photo> result = tempFlickr.getPhotosInterface().search(para, 500, offset);
//            logger.log(Level.INFO, "invoke photoInterface.search complete");
//            //System.out.println("Result size = "+result.size());
//            if(result == null || result.size()==0){break;}
//            offset++;
//            results.addAll(result);
//        }
//        logger.log(Level.DEBUG, "Found phot size = "+results.size());
        ArrayList<Photo> results = this.getTagManager().SearchPhotoByTags(tags, uid);
        return results;

    }

    public ArrayList<PhotoWrapper> queryPhotoInPhotoset(PhotoSetWrapper psw) throws FlickrException {
        Photoset ps = psw.getPhotoSet();
        logger.log(Level.DEBUG, "Now Processing photoset [" + ps.getTitle()+"]");
        ArrayList<PhotoWrapper> finalList = new ArrayList<PhotoWrapper>();
        
        //catch mechaniasm
        if(psw.isPhotoListGot()){
            logger.log(Level.INFO, "Photo of Photoset["+psw.toString()+"] is alread got, return photolist directly instead of query again");
            finalList = psw.getPhotos();
        }else{
            int offset = 1;
            while(true){
                //System.out.println("Offset = "+ offset);
                logger.log(Level.INFO, "Invoke PhotosetInterface.getPhotos...PhotoSet ID ="+ps.getId()+", page = "+offset);
                PhotoList<Photo> bufferPhotoList = this.getPhotosetInterface().getPhotos(ps.getId(), 500, offset++);
                logger.log(Level.INFO, "...Complete");
                for(Photo p:bufferPhotoList){
                    PhotoWrapper pw = new PhotoWrapper();
                    pw.setPhoto(p);
                    finalList.add(pw);
                }
                //finalList.addAll();
                if(finalList.size() >= ps.getPhotoCount()){break;}
            }
            psw.setPhotos(finalList);
            psw.setPhotoListGot(true);
        }
        return finalList;
    }


    public Comm.RetrunCode queryExtraPhotoInfo(ArrayList<PhotoWrapper> selectedPhoto){
        int i=0;
        this.STATUS = FlickrMiner.STATUS_READY;
        boolean retryState = false;
        int retryCount = 3;
        Comm.RetrunCode rc = Comm.RetrunCode.SUCCESS;
        currentProcessedPhotoExifCount = 0;
        logger.log(Level.DEBUG, "Fetch extra data starts");
        //for(PhotoWrapper p: allPhotoWrapperList){
        for(i=0 ;  i<selectedPhoto.size() ; i++){
            PhotoWrapper p = selectedPhoto.get(i);
            Photo photo = p.getPhoto();
//            if(p.hasExif()){
//                logger.log(Level.DEBUG, p.getPhoto().getTitle()+" is already has Exif Data");
//                continue;
//            }
            //logger.log(Level.DEBUG, "URL = " + p.getTitle());
            //reset retry count
            if(!retryState){
                retryCount = 3 ;
            }else{
                logger.log(Level.ERROR, "Retry for photo["+photo.getTitle()+"]...remaining = "+retryCount);
            }
			//要透過 getInfo() 後，才能透過 Photo.getDescption() 拿資料
            if(retryCount <=0){
                logger.log(Level.ERROR, "After retry 3 times, it is still failed to get photo info of ["+photo.getTitle()+"], so skip it!");
                currentProcessedPhotoExifCount++; //even failure, still need to add by 1
                TOTAL_FAIL_COUNT_ON_EXIF_FETCHING++;
                continue;
            }
            PhotoMeta meta = new PhotoMeta();
            p.setMeta(meta);
            ArrayList<Exif> aryExif = new ArrayList<Exif>();            
            try {
                logger.log(Level.DEBUG, "Invoke PhotoInterface.getInfo()...Photo ID = "+photo.getId());
                p.setPhoto(this.getPhotosInterface().getInfo(photo.getId(), null));
                logger.log(Level.DEBUG, "Invoke PhotoInterface.getInfo()...Photo ID = "+photo.getId()+"...Complete");                
                aryExif = (ArrayList<Exif>) this.getPhotosInterface().getExif(p.getPhoto().getId(), null);
                //if(this.photosInterface.getAllContexts(photo.getId()).size()>0){
                if(this.getPhotosInterface().getAllContexts(photo.getId()).getPhotoSetList().size()>0){
                    //String photoSetTitle = this.photosInterface.getAllContexts(photo.getId()).get(0).getTitle();
                    //String photoSetId = this.photosInterface.getAllContexts(photo.getId()).get(0).getId();
                    String photoSetTitle = this.getPhotosInterface().getAllContexts(photo.getId()).getPhotoSetList().get(0).getTitle();
                    String photoSetId = this.getPhotosInterface().getAllContexts(photo.getId()).getPhotoSetList().get(0).getId();
                    meta.setPhotoSetId(photoSetId);
                    meta.setPhotoSetTitle(photoSetTitle);
                    p.setHasExifData(true);
                }
                retryState = false;
            } catch (FlickrException e){
                TOTAL_FAIL_COUNT_ON_EXIF_FETCHING++;
                logger.log(Level.INFO, e.getErrorCode()+":"+e.getErrorMessage());
            } catch (Exception ex) {
                logger.log(Level.ERROR, "Flickr API calling error on Photo:"+p.getPhoto().getId());
                logger.log(Level.DEBUG, ex, ex.fillInStackTrace());
                i--;
                retryCount--;
                retryState = true;                                           
                continue;
                //this.STATUS = FlickrMiner.STATUS_FINISH;
                //return ;
            }
                //HashMap<String, String> hashExif = new HashMap<String, String>();
                //if(Comm.DEBUG){
                    currentProcessedPhotoExifCount++;
//                    System.out.println("===========================================");
                    logger.log(Level.DEBUG, "Put Exif List to Photo ["+p.getPhoto().getId()+"], Already got = " + (currentProcessedPhotoExifCount));
//                    logger.log(Level.DEBUG, "Photo Url = "+ p.getUrl());
//                    logger.log(Level.DEBUG, "Exif size = "+ aryExif.size());
//                  }
                    logger.info("Dump exif info of Photo ID "+p.getPhoto().getId());
                    if(aryExif.size() ==0){
                        logger.error("exif size =0");
                    }
                    for(Exif e : aryExif){
                        // reduce memory usage, only add the wanted item
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_APERTURE)){
                            logger.info("Aperture = "+e.getRaw());
                            meta.getExifDataWrapper().setAperture(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_EXPOSURE)){
                            logger.info("Exposure = "+e.getRaw());
                            meta.getExifDataWrapper().setExposure(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_FOCAL_LENGTH)){
                            logger.info("Focal Length = "+e.getRaw());
                            meta.getExifDataWrapper().setFocalLength(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_ISOSPEED)){
                            logger.info("ISO speed = "+e.getRaw());
                            meta.getExifDataWrapper().setIsoSpeed(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_MODEL)){
                            logger.info("Model = "+e.getRaw());
                            meta.getExifDataWrapper().setCameraModel(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_LENS) || e.getLabel().equalsIgnoreCase(Comm.EXIF_LENS_2)){
                            logger.info("Lens = "+e.getRaw());
                            meta.getExifDataWrapper().setLensModel(e.getRaw());
                        }
                        if(e.getLabel().equalsIgnoreCase(Comm.EXIF_DATE_TAKEN)){
                            logger.info("Date = "+e.getRaw());
                            meta.getExifDataWrapper().setDateTaken(e.getRaw());
                        }
                    }
                    //photExifMap.put(p.getPhoto().getId(), hashExif);
                    //this.userWrapper.setPhotoExifMap(photExifMap);  //每做完一個loop就先set一次，可讓之後的UI即時更新
        }
        logger.log(Level.DEBUG, "Fetch extra data ends");
        //this.userWrapper.setPhotoExifMap(photExifMap);       
        this.STATUS = FlickrMiner.STATUS_FINISH;
        return Comm.RetrunCode.SUCCESS;
    }

    /**
     * @return the tagManager
     */
    private TagManager getTagManager() {
        return tagManager;
    }

    /**
     * @return the authManager
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * @param authManager the authManager to set
     */
    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public String uploadImage(File f, UploadMetaData meta, String photoSetId) throws FlickrException, IOException, SAXException, URISyntaxException{
        authManager.authorizeGuiEx(Permission.WRITE);
        //caller should make sure the permission of WRITE
        String pid = "";
        logger.log(Level.DEBUG, "Start to upload photo "+f.getAbsolutePath());
        pid = this.uploader.upload(f, meta);
        logger.log(Level.DEBUG, "End to upload photo "+f.getAbsolutePath()+", Picture ID = " + pid);

        if(photoSetId != null && !photoSetId.equalsIgnoreCase("") && pid!=null){
            logger.log(Level.DEBUG, "Add Photo["+pid+"] to PhotoSet["+photoSetId+"]");
            this.getPhotosetInterface().addPhoto(photoSetId, pid);
        }
        return pid;
    }

    /* test function to upload photo, it can work! */
    private void testUpload() {
        Thread t = new Thread(){
            public String tokenId = "";
            public void run(){
                try {
                    authManager.authorizeGuiEx(Permission.WRITE);
                    Uploader up = flickr.getUploader();
                    UploadMetaData meta = new UploadMetaData();
                    meta.setTitle("test");
                    File f = new File("c:\\test.png");
                    tokenId = up.upload(f, meta);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                } catch (SAXException ex) {
                    java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                } catch (URISyntaxException ex) {
                    java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                } catch (FlickrException ex) {
                    java.util.logging.Logger.getLogger(FlickrMiner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }

    /**
     * @return the photosInterface
     */
    public PhotosInterface getPhotosInterface() {
        return photosInterface;
    }

    /**
     * @return the photosetInterface
     */
    public PhotosetsInterface getPhotosetInterface() {
        return photosetInterface;
    }
}


//    public void printInfo() throws Exception {
//        RequestContext rc = RequestContext.getRequestContext();
//
//        if (this.authStore != null) {
//            auth = this.authStore.retrieve(this.nsid);
//            if (auth == null) {
//                this.authorize();
//                this.printInfo();
//            } else {
//                rc.setAuth(auth);
//                System.out.println("nsid: " + auth.getUser().getId());
//                System.out.println("Realname: " + auth.getUser().getRealName());
//                System.out.println("Username: " + auth.getUser().getUsername());
//                System.out.println("Permission: " + auth.getPermission().getType());
//
//                PhotosetsInterface pi = flickr.getPhotosetsInterface();
//                PhotosInterface photoInt = flickr.getPhotosInterface();
//
//
//                Photosets photoSets = pi.getList(nsid, Integer.MAX_VALUE, 1);
//                ArrayList<Photoset> aryPhotoset = (ArrayList<Photoset>) photoSets.getPhotosets();
//                ArrayList<Photo> aryAllPhotos = new ArrayList<Photo>();
//
//                for(Photoset photoSet : aryPhotoset){
//                    System.out.println("Title = " + photoSet.getTitle());
//                    System.out.println("URL = " + photoSet.getUrl());
//                    //aryAllPhotos.addAll(pi.getPhotos(photoSet.getId(), Integer.MAX_VALUE, 1));
//                    for(Photo p : pi.getPhotos(photoSet.getId(), Integer.MAX_VALUE, 1)){
//                        System.out.println("Photo ID = "+ p.getId());
//                        System.out.println("Photo Title = "+ p.getTitle());
//                        System.out.println("Photo URL = "+ p.getUrl());
//
//                    }
//                }
//
//            }
//        }
//    }