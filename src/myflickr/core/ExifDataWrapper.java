/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

/**
 *
 * @author neil
 */
public class ExifDataWrapper {
    private String CameraModel = Comm.NO_DATA;
    private String LensModel = Comm.NO_DATA;
    private String FocalLength = Comm.NO_DATA;
    private String Aperture = Comm.NO_DATA;
    private String Exposure = Comm.NO_DATA;
    private String IsoSpeed = Comm.NO_DATA;
    private String DateTaken = Comm.NO_DATA;

    /**
     * @return the CameraModel
     */
    public String getCameraModel() {
        return CameraModel;
    }

    /**
     * @param CameraModel the CameraModel to set
     */
    public void setCameraModel(String CameraModel) {
        this.CameraModel = CameraModel;
    }

    /**
     * @return the LensModel
     */
    public String getLensModel() {
        return LensModel;
    }

    /**
     * @param LensModel the LensModel to set
     */
    public void setLensModel(String LensModel) {
        LensModel = _normalize(LensModel);
        this.LensModel = LensModel;
    }

    /**
     * @return the FocalLength
     */
    public String getFocalLength() {
        return FocalLength;
    }

    /**
     * @param FocalLength the FocalLength to set
     */
    public void setFocalLength(String FocalLength) {
        FocalLength = _normalize(FocalLength);
        this.FocalLength = FocalLength;
    }

    /**
     * @return the Aperture
     */
    public String getAperture() {
        return Aperture;
    }

    /**
     * @param Aperture the Aperture to set
     */
    public void setAperture(String Aperture) {
        Aperture = _normalize(Aperture);
        this.Aperture = Aperture;
    }

    /**
     * @return the Exposure
     */
    public String getExposure() {
        return Exposure;
    }

    /**
     * @param Exposure the Exposure to set
     */
    public void setExposure(String Exposure) {
        Exposure = _normalize(Exposure);
        this.Exposure = Exposure;
    }

    /**
     * @return the IsoSpeed
     */
    public String getIsoSpeed() {
        return IsoSpeed;
    }

    /**
     * @param IsoSpeed the IsoSpeed to set
     */
    public void setIsoSpeed(String IsoSpeed) {
        IsoSpeed = _normalize(IsoSpeed);
        this.IsoSpeed = IsoSpeed;
    }

    private String _normalize(String str) {        
        str = str.replaceAll("\\.0+", "");
        str = str.replaceAll("\\smm", "mm");
        return str;
    }

    /**
     * @return the DateTaken
     */
    public String getDateTaken() {
        return DateTaken;
    }

    /**
     * @param DateTaken the DateTaken to set
     */
    public void setDateTaken(String DateTaken) {
        this.DateTaken = DateTaken;
    }
}
