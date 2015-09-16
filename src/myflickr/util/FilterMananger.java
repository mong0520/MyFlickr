/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;


import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.tags.Tag;
import java.util.ArrayList;
import java.util.Iterator;
import myflickr.FilterSpec;
import myflickr.core.Comm;
import myflickr.core.PhotoWrapper;
import myflickr.core.UserWrapper;

/**
 *
 * @author Mong
 */
public class FilterMananger {
   private FilterSpec filterSpec;
   private boolean isBackupable = true;;
   private ArrayList<PhotoWrapper> backupOriginalPhotoList;
   private ArrayList<PhotoWrapper> filteredPhoto;

    public FilterMananger(){
        filteredPhoto = new ArrayList<PhotoWrapper>();
    }

    public FilterMananger(FilterSpec f, UserWrapper u){
        this();
        this.filterSpec = f;        
    }

    /**
     * @param filterSpec the filterSpec to set
     */
    public void setFilterSpec(FilterSpec filterSpec) {
        this.filterSpec = filterSpec;
    }

    /**
     * @return the filteredPhotoUrlList
     */
    public /*ArrayList<PhotoWrapper>*/ void filterPhoto(ArrayList<PhotoWrapper> photoList, FilterSpec filterSpec) {
        if(filteredPhoto != null){filteredPhoto.clear();}
        //if(Comm.DEBUG){System.out.println("[Debug] getFilteredPhotoUrlListBySpec is called");})                
        if(photoList==null || photoList.size()==0){return ;}
        _resetList();
        for(PhotoWrapper p : photoList){
//            if(Comm.DEBUG){System.out.println("[Debug] Now Checking..."+p.getUrl());}
            boolean bolMatchCameraSpec = _matchCameraSpec(p, filterSpec.getSelectedCameraModel());
            boolean bolMatchLensId = _matchLensIdSpec(p, filterSpec.getSelectedLensId());
            boolean bolMatchFocalLength = _matchFocalLengthSpec(p, filterSpec.getSelectedFocalLength());
            boolean bolMatchAperture = _matchApertureSpec(p, filterSpec.getSelectedAperture());
            boolean bolMatchShutter = _matchShutterSpec(p, filterSpec.getSelectedShutter());
            boolean bolMatchIso = _matchIsoSpec(p, filterSpec.getSelectedIso());

//            if(Comm.DEBUG){
//                System.out.println("bolMatchCameraSpec = "+ bolMatchCameraSpec);
//                System.out.println("bolMatchLensId = "+ bolMatchLensId);
//                System.out.println("bolMatchFocalLength = "+ bolMatchFocalLength);
//                System.out.println("bolMatchAperture = "+ bolMatchAperture);
//                System.out.println("bolMatchShutter = "+ bolMatchShutter);
//                System.out.println("bolMatchIso = "+ bolMatchIso);
//            }

            if(bolMatchCameraSpec && bolMatchLensId && bolMatchFocalLength &&  bolMatchAperture && bolMatchShutter &&bolMatchIso){
                //filteredPhoto.add(p);
                p.setVisiable(true);
            }else{
                p.setVisiable(false);
            }
        }
        //return filteredPhoto;
    }
    private boolean _matchCameraSpec(PhotoWrapper p, ArrayList<String> list){
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;        
        for(String str :list ){
            if(Comm.DEBUG){
//                System.out.println("[Debug] Camera Model source = "+this.userWrapper.getExifData(p, Comm.EXIF_MODEL));
//                System.out.println("[Debug] Camera Model target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getCameraModel())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private boolean _matchLensIdSpec(PhotoWrapper p, ArrayList<String> list) {
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;
        for(String str :list ){
            if(Comm.DEBUG){
                //System.out.println("[Debug] LensId source = "+p.getExifDescription().getLensId());
                //System.out.println("[Debug] LensId target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getLensModel())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private boolean _matchFocalLengthSpec(PhotoWrapper p, ArrayList<String> list) {
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;
        for(String str :list ){
            if(Comm.DEBUG){
//                System.out.println("[Debug] FocalLength source = "+this.userWrapper.getExifData(p, Comm.EXIF_FOCAL_LENGTH));
//                System.out.println("[Debug] FocalLength target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getFocalLength())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private boolean _matchApertureSpec(PhotoWrapper p, ArrayList<String> list) {
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;
        for(String str :list ){
            if(Comm.DEBUG){
                //System.out.println("[Debug] Aperture source = "+p.getExifDescription().getAperture());
                //System.out.println("[Debug] Aperture target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getAperture())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private boolean _matchShutterSpec(PhotoWrapper p, ArrayList<String> list) {
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;
        for(String str :list ){
            if(Comm.DEBUG){
                //System.out.println("[Debug] Shutter source = "+p.getExifDescription().getShutter());
                //System.out.println("[Debug] Shutter target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getExposure())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private boolean _matchIsoSpec(PhotoWrapper p, ArrayList<String> list) {
        //list is from  filterSpec.getSelectedXxxxx(); this is what user selected from jList
        boolean r = false;
        for(String str :list ){
            if(Comm.DEBUG){
                //System.out.println("[Debug] Iso source = "+p.getExifDescription().getIso());
                //System.out.println("[Debug] Iso target = "+str);
            }
            if(str.equalsIgnoreCase(p.getMeta().getExifDataWrapper().getIsoSpeed())){
                //System.out.println("Matched");
                r = true;
                return r;
            }
        }
        return r;
    }

    private void _resetList() {
        this.filteredPhoto.clear();
    }

    //backup original list
    public void setOriginalPhotoList(ArrayList<PhotoWrapper> list){
        this.backupOriginalPhotoList = list;
    }
    public ArrayList<PhotoWrapper> getOriginalPhotoList(){
        return backupOriginalPhotoList;
    }

//    public ArrayList<PhotoWrapper> getFilteredPhotoByTag(Object[] selectedValues, ArrayList<PhotoWrapper> photoWrapperList) {
//        ArrayList<PhotoWrapper> list = new ArrayList<PhotoWrapper>();
//        boolean found  = false;
//        for(PhotoWrapper pw : photoWrapperList){
//            found  = false;
//            Iterator<Tag> tags = pw.getPhoto().getTags().iterator();
//            while(tags.hasNext()){ //tags for each photo
//                Tag tagOfPhoto = tags.next();
//                for(Object o : selectedValues){ //tags for selected value
//                    TagWrapper tagOfSelected = (TagWrapper)o;
//                    if(tagOfPhoto.getRaw().equalsIgnoreCase(tagOfSelected.toString())){
//                        list.add(pw);
//                        found  = true;
//                        break;
//                    }
//                }
//                if(found){break;}
//            }
//        }
//        return list;
//    }

  }

