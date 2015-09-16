/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.photos.Photo;
import java.util.*;

/**
 *
 * @author Mong
 */
public class StatisticData{

    /**
     * @return the photoUrlList
     */
    public ArrayList<String> getPhotoUrlList() {
        return photoUrlList;
    }


    private ArrayList<String> cameraList;
    private ArrayList<String> isoList;
    private ArrayList<String> apertureList;
    private ArrayList<String> shutterList;
    private ArrayList<String> lensIdList;
    private ArrayList<String> focalList;
    private ArrayList<String> photoUrlList;

    public Comm.RetrunCode initStatisticData(ArrayList<PhotoWrapper> list){
        //System.out.println("Init StatisticData");
        ExifComparator exifComparator = new ExifComparator();
//        if(u == null){
//            System.out.println("[Debug] user is null");
//            return Comm.RetrunCode.ERROR;
//        }
//        if(u.getPhotoWrapperList().isEmpty()){
//            System.out.println("[Debug] user's photo list is empty");
//            return Comm.RetrunCode.ERROR;
//        }
        if(list == null  || list.size()==0 || list.isEmpty()){
            return Comm.RetrunCode.ERROR;
        }
        cameraList = new ArrayList<String>();
        isoList = new ArrayList<String>();
        apertureList = new ArrayList<String>();
        shutterList = new ArrayList<String>();
        lensIdList = new ArrayList<String>();
        focalList = new ArrayList<String>();
        photoUrlList = new ArrayList<String>();

        //ExifComparator exifComparator = new ExifComparator();

        for(PhotoWrapper p : list){
            //ExifDescription exif = p.getExifDescription();
            if(!cameraList.contains(p.getMeta().getExifDataWrapper().getCameraModel())){/*System.out.println("Camera Add: "+u.getExifData(p, Comm.EXIF_MODEL));*/cameraList.add(p.getMeta().getExifDataWrapper().getCameraModel());}
            if(!apertureList.contains(p.getMeta().getExifDataWrapper().getAperture())){/*System.out.println("Aperture Add: "+u.getExifData(p, Comm.EXIF_APERTURE));*/apertureList.add(p.getMeta().getExifDataWrapper().getAperture());}
            if(!shutterList.contains(p.getMeta().getExifDataWrapper().getExposure())){/*System.out.println("Shutter Add: "+u.getExifData(p, Comm.EXIF_EXPOSURE));*/shutterList.add(p.getMeta().getExifDataWrapper().getExposure());}
            if(!lensIdList.contains(p.getMeta().getExifDataWrapper().getLensModel())){/*System.out.println("Lens Add: "+u.getExifData(p, Comm.EXIF_LENS));*/lensIdList.add(p.getMeta().getExifDataWrapper().getLensModel());}
            if(!focalList.contains(p.getMeta().getExifDataWrapper().getFocalLength())){/*System.out.println("Focal Add: "+u.getExifData(p, Comm.EXIF_FOCAL_LENGTH));*/focalList.add(p.getMeta().getExifDataWrapper().getFocalLength());}
            if(!isoList.contains(p.getMeta().getExifDataWrapper().getIsoSpeed())){/*System.out.println("Iso Add: "+u.getExifData(p, Comm.EXIF_ISOSPEED));*/isoList.add(p.getMeta().getExifDataWrapper().getIsoSpeed());}
            photoUrlList.add(p.getPhoto().getUrl());

        }
        Collections.sort(cameraList);
        Collections.sort(lensIdList);
        Collections.sort(focalList, exifComparator);
        Collections.sort(apertureList, exifComparator);
        Collections.sort(shutterList, exifComparator);
        Collections.sort(isoList, exifComparator);

        return Comm.RetrunCode.SUCCESS;
    }

    public ArrayList<String> getCameraList(){
        return this.cameraList;
    }
    public ArrayList<String> getIsoaList(){
        return this.isoList;
    }
    public ArrayList<String> getApertureList(){
        return this.apertureList;
    }
    public ArrayList<String> getShutterList(){
        return this.shutterList;
    }
    public ArrayList<String> getLensIDList(){
        return this.lensIdList;
    }
    public ArrayList<String> getFocalLengthList(){
        return this.focalList;
    }
}
