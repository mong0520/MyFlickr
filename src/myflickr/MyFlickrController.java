/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myflickr;

import java.net.URISyntaxException;
import java.util.Collection;
import org.apache.log4j.*;
import com.flickr4java.flickr.*;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.tags.Tag;
import com.flickr4java.flickr.uploader.UploadMetaData;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import myflickr.core.*;
import myflickr.util.HtmlReportGenerator;
import myflickr.util.MyFlickrLogManager;
import org.xml.sax.SAXException;

/**
 *
 * @author neil
 */
public class MyFlickrController {
    HashMap<String, UserWrapper> userWrapperPool;
    UserWrapper user;
    FlickrMiner miner;
    HtmlReportGenerator htmlGen;
    int photoCountToBeGot;
    private Logger logger ;    
    HashMap<File, UploadMetaData> uploadMetaMap;
    HashMap<File, PhotoSetWrapper> photosetMap; //file to PhotoSetWrapper mapping, object could be a string to represent no need to add to any photoset

    private ArrayList<PhotoWrapper> backupList;  //use for store temp lsit

    public MyFlickrController() {
        userWrapperPool = new HashMap<String, UserWrapper>();
        htmlGen = new HtmlReportGenerator();
        logger = MyFlickrLogManager.getLogger();
        uploadMetaMap = new HashMap<File, UploadMetaData>();
        photosetMap = new HashMap<File, PhotoSetWrapper>();
    }

    public UserWrapper initUser(String username) throws FlickrException, IOException, SAXException, URISyntaxException {
            //boolean buildExif = true;
        if(userWrapperPool.containsKey(username)){
            logger.info(username +" is existed in the pool, return it directly");
            return userWrapperPool.get(username);
        }
        user = new UserWrapper(username);
        miner = new FlickrMiner("5d3fd4c39ad5005fb9547fc540647cf9", "83d46646050639ab", user);
        //miner.buildUserData(buildExif);
        if (miner.initUser() == Comm.RetrunCode.SUCCESS) {
            logger.info("Add "+ username +" into UserWrapper Pool, current size = "+userWrapperPool.size());
            userWrapperPool.put(username, user);
            return user;
        } else {
            logger.error("init user failed");
            return null;
        }
    }

    public String queryPathAliasName(String uid) throws FlickrException{
        return this.miner.queryPathAliasName(uid);
    }

//    public String getAuthUrl(Permission p){
//        try {
//            return miner.getAuthUrl(p);
//        } catch (FlickrException ex) {
//            Logger.getLogger(MyFlickrController.class.getName()).log(Level.ERROR, null, ex);
//            return null;
//        }
//    }

//    public Comm.RetrunCode doAuth(String code) throws IOException, FlickrException{
//            return miner.doAuth(code);
//    }

//    public Comm.RetrunCode authorize() throws IOException, FlickrException, SAXException, URISyntaxException{
//        return miner.getAuthManager().authorizeGui();
//    }
    
    public Comm.RetrunCode authorizeEx(Permission p) throws IOException, FlickrException, SAXException, URISyntaxException{
        return miner.getAuthManager().authorizeGuiEx(p);
    }

    public Permission getPermissionType(){
        return miner.getAuthManager().getPermissionType();
    }

//    public Comm.RetrunCode buildUserData(int rangeToList, boolean bExif) {
//        this.photoCountToBeGot = rangeToList;
//        return miner.buildUserData(rangeToList, bExif);
//    }
    public Comm.RetrunCode buildUserDataEx(int rangeToList, boolean bExif, boolean withPrivate) {
        this.photoCountToBeGot = rangeToList;
        return miner.buildUserDataEx(rangeToList, bExif, withPrivate);
    }

    public ArrayList<PhotoWrapper> getPhotosByGruop(String gid) throws FlickrException{
        return this.miner.getPhotosByGruop(gid);
    }

    public int queryPhotoCountToBeGot(){
        return this.photoCountToBeGot;
    }

    public void generatorHtml(String userID, ArrayList<PhotoWrapper> selectedPhoto, Comm.PhotoSize size, boolean hyperlink) {
        try {
            if (htmlGen.genHtmlByPhotoList(userID, selectedPhoto, size, hyperlink) == Comm.RetrunCode.SUCCESS) {
            }
        } catch (FlickrException ex) {
            Logger.getLogger(MyFlickrView.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public Comm.RetrunCode generatorHtml(String userID, ArrayList<PhotoWrapper> selectedPhoto, Comm.PhotoSize size,
            boolean title, boolean description, boolean camera, boolean lens, boolean aperture, boolean exposure, boolean focalLength, boolean iso, boolean hyperlink, boolean bHighRes) {                        
        try
        {
            //in order to get the actual url of photo, but may impact performace
//            for(PhotoWrapper pw: selectedPhoto){
//                Photo p =pw.getPhoto();
//                Collection<Size> sizes = this.miner.getPhotosInterface().getSizes(p.getId(), true);
//                p.setSizes(sizes);
//            }
            if (htmlGen.genHtmlByPhotoList(userID, selectedPhoto, size, title, description, camera, lens, aperture, exposure, focalLength, iso, hyperlink, bHighRes, this.miner.getPhotosInterface()) == Comm.RetrunCode.SUCCESS) {
                //success message
                return Comm.RetrunCode.SUCCESS;
            }else{
                return Comm.RetrunCode.ERROR;
            }
        } catch (FlickrException ex) {
            Logger.getLogger(MyFlickrView.class.getName()).log(Level.ERROR, null, ex);
            return Comm.RetrunCode.ERROR;
        }
    }

    public String getHtmlSourceCode(){
        return htmlGen.getHtmlSourceCode();
    }
    public int queryCurrentProcessedPhotoCount() {
        return this.miner.queryCurrentProcessedPhotoStatus();
    }

    public int queryCurrentProcessedExifPhotoCount() {
        return this.miner.queryCurrentProcessedExifPhotoStatus();
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return miner.queryStatus();
    }

    public Comm.RetrunCode setTag(Photo p, String[] tags) throws FlickrException {
        if(this.miner.setTagsToPhoto(p.getId(), tags) == Comm.RetrunCode.SUCCESS){
            return Comm.RetrunCode.SUCCESS;
        }else{
            return Comm.RetrunCode.ERROR;
        }
    }

    public Comm.RetrunCode addTag(Photo p, String[] tags) throws FlickrException {
        //System.out.println("[Debug] Permission = " + this.getPermissionType().getType());
        if(miner.addTagsToPhoto(p.getId(), tags) == Comm.RetrunCode.SUCCESS){
            return Comm.RetrunCode.SUCCESS;
        }else{
            return Comm.RetrunCode.ERROR;
        }
    }
    public ArrayList<Tag> getTagsByPhoto(Photo p) {
        return this.miner.getTagsByPhoto(p.getId());
        //return  (ArrayList<Tag>) p.getTags();
    }

    public void updatePhoto(PhotoWrapper selectedPhoto) {
        Photo p = miner.getPhotoInstance(selectedPhoto.getPhoto().getId());
        selectedPhoto.setPhoto(p);
    }


//    public void buildTagSetToUser(){
//        this.user.setTagSet(miner.getTagSetByUser(this.user.getUser().getId()));
//    }

    public ArrayList<PhotoWrapper> SearchPhotoByTag(String[] para) throws FlickrException{
        ArrayList<PhotoWrapper> results_wrapper = new ArrayList<PhotoWrapper>();
        ArrayList<Photo> results = miner.SearchPhotoByTags(para, this.user.getUser().getId());
        for(Photo p : results){
            PhotoWrapper pw = new PhotoWrapper();
            pw.setPhoto(p);
            results_wrapper.add(pw);
        }
        return results_wrapper;
    }

    public ArrayList<PhotoWrapper> queryPhotoInPhotoset(PhotoSetWrapper psw) throws FlickrException {                
        ArrayList<PhotoWrapper> results = miner.queryPhotoInPhotoset(psw);
        return results;
    }
    
    public String queryPhotosetNameByPhotoId(String pid) throws FlickrException{
        return this.miner.queryPhotoSetByPhotoId(pid);
    }

     public ArrayList<Size> getAvaliableSize(String pid) throws FlickrException{
        return miner.getAvaliableSize(pid);
    }

     public String getLargestSizePhotoUrl(String pid, boolean skipOriginal) throws FlickrException{
         ArrayList<Size> arySize = this.getAvaliableSize(pid);
         String targetUrl = "";
         if(arySize !=null){
             if(skipOriginal){
                 for(int i=arySize.size()-1;i >= 0 ;i--){
                     if(arySize.get(i).getLabel() == Size.ORIGINAL){continue;}
                     else{
                         targetUrl = arySize.get(i).getSource();
                         break;}
                 }
                 return targetUrl;
             }else{
                targetUrl =arySize.get(arySize.size()-1).getSource();
                return targetUrl;
             }
         }else{
             return null;
         }
     }

      public String getLargeSizePhotoUrl(String pid) throws FlickrException{
         ArrayList<Size> arySize = this.getAvaliableSize(pid);
         String src = null;
         if(arySize !=null){
             src = arySize.get(arySize.size()-1).getSource(); //default is the largest
             for(Size s: arySize){
                 //如果沒有large, 就下載最大的，但不可能是original size, 因為original size 一定有原圖
                 if (s.getLabel() == Size.LARGE){
                     src = s.getSource();
                     break;
                 }
             }
         }else{
             return null;
         }
         return src;
     }


    public Comm.RetrunCode fetchExifData(ArrayList<PhotoWrapper> selectedPhoto) {
        return this.miner.queryExtraPhotoInfo(selectedPhoto);
    }

    public Comm.RetrunCode fetchExifData(ArrayList<PhotoWrapper> selectedPhoto, int threadCount){
        //可改用multi thread加快exif取得，以下的code可以成常執行，但status的顯示方式還要再調，問題：只要有一個thread做完事，status 就會停止更新

        if(threadCount > selectedPhoto.size()){
            threadCount = selectedPhoto.size();
        }
        //construst
        ArrayList<ArrayList<PhotoWrapper>> photoList_for_thread = new ArrayList<ArrayList<PhotoWrapper>>(threadCount);
        for(int i=0;i<threadCount;i++){
            photoList_for_thread.add(new ArrayList<PhotoWrapper>());
        }
        ExifWorkerThread[] t = new ExifWorkerThread[threadCount];

        //dispatch working thread
        for(int i=0;i<selectedPhoto.size();i++){
            for(int j=0;j<threadCount;j++)
            if(i%threadCount == j){
                photoList_for_thread.get(j).add(selectedPhoto.get(i));
            }            
        }

        //start working thread
        for(int i=0; i<threadCount ; i++){
            t[i] = new ExifWorkerThread();
            t[i].setList(photoList_for_thread.get(i));
            logger.log(Level.INFO, "Thread["+i+"] is started to get Exif");
            t[i].start();
        }
        for(int i=0; i<threadCount ; i++){            
            try {
                t[i].join();
            } catch (InterruptedException ex) {
                logger.log(Level.ERROR, ex);
            }
        }
        return Comm.RetrunCode.SUCCESS;
    }

    public int queryCurrentStatus(){
        return this.miner.STATUS;
    }
    /**
     * @return the backupList
     */
    public ArrayList<PhotoWrapper> getBackupList() {
        return backupList;
    }

    /**
     * @param backupList the backupList to set
     */
    public void setBackupList(ArrayList<PhotoWrapper> backupList) {
        this.backupList = backupList;
    }

    public void updateUserTagSet() throws FlickrException{
        HashSet<String> tagSet = miner.getTagSetByUser(this.user.getUser().getId());
        this.user.setTagSet(tagSet);
    }

    public void setPhotoUploadToPhotoSet(File f, PhotoSetWrapper selectedItem) {
        this.photosetMap.put(f, selectedItem);
    }

    public PhotoSetWrapper getPhotoUploadToPhotoSet(File f) {
        if(this.photosetMap.get(f)!=null){
            return (PhotoSetWrapper) this.photosetMap.get(f);
        }else{
            return null;
        }
    }

    public void setPhotoUploadMeta(File f, UploadMetaData meta){
        this.uploadMetaMap.put(f, meta);
    }

    public UploadMetaData getUploadMeta(File f){
        if(this.uploadMetaMap.containsKey(f)){
            return this.uploadMetaMap.get(f);
        }else{
            this.setPhotoUploadMeta(f, new UploadMetaData());
            return this.getUploadMeta(f);
        }
    }

    public String uploadPhoto(File f, UploadMetaData meta,  String photoSetId) throws FlickrException, IOException, SAXException, URISyntaxException{
        meta.setPublicFlag(true); //force to public
        return this.miner.uploadImage(f, meta, photoSetId);
    }

    public void cleanUploadMeta(){
        this.uploadMetaMap.clear();
        logger.log(Level.INFO, "Clean up upload batch list");
    }

    public PhotoSetWrapper createNewPhotoSetWrapper(String title, String pid, boolean CreateNewPhotoSet){
        PhotoSetWrapper psw = new PhotoSetWrapper();
        psw.setCreateNewPhotoSet(CreateNewPhotoSet);
        Photoset ps = new Photoset();
        ps.setTitle(title);
        ps.setId(pid);
        psw.setPhotoSet(ps);
        return psw;
    }

    String getPhotoStaticUrlBySize(int size, String pid) throws FlickrException {
        ArrayList<Size> arySize = this.getAvaliableSize(pid);
         String src = arySize.get(arySize.size()-1).getSource(); //default is the largest
         //如果沒有large, 就下載最大的，但不可能是original size, 因為original size 一定有原圖
         if(arySize !=null){
             for(Size s: arySize){
                 if (s.getLabel() == size){
                     src = s.getSource();
                     break;
                 }
             }
         }else{
             return null;
         }
         return src;
    }

    ArrayList<PhotoWrapper> getUserFavoritePhoto(String uid) throws FlickrException {
        return miner.getFavoritePhotoByUser(uid);
    }

    public String createNewPhotoSet(String newPhotoSetTitle, String pid) throws FlickrException {
        PhotosetsInterface psi = this.miner.getPhotosetInterface();
        Photoset pw = psi.create(newPhotoSetTitle, "", pid);        
        logger.log(Level.INFO, "PhotoSet["+newPhotoSetTitle+"] is created, PhotoSetID=["+pw.getId()+"]");
        logger.log(Level.DEBUG, "Set PhotoID["+pid+"] as primary photo");
        return pw.getId();
    }

    void assignPhotoToSet(String pid, String SelectedPSID) throws FlickrException {
        PhotosetsInterface psi = this.miner.getPhotosetInterface();
        psi.addPhoto(SelectedPSID, pid);
    }

    public class ExifWorkerThread extends Thread{

        ArrayList<PhotoWrapper> list ;
        Comm.RetrunCode result;
        public void setList(ArrayList<PhotoWrapper> l){
            this.list = l;
        }
        public Comm.RetrunCode queryResult(){
            return this.result;
        }
        public void run() {
            result = miner.queryExtraPhotoInfo(list);
        }
    }
}
