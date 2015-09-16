/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr;

import java.util.ArrayList;

/**
 *
 * @author Mong
 */
public class FilterSpec {
    private ArrayList<String> selectedCameraModel;
    private ArrayList<String> selectedLensId;
    private ArrayList<String> selectedFocalLength;
    private ArrayList<String> selectedAperture;
    private ArrayList<String> selectedShutter;
    private ArrayList<String> selectedIso;


    public FilterSpec(){
        selectedCameraModel = new ArrayList<String>();
        selectedLensId = new ArrayList<String>();
        selectedFocalLength = new ArrayList<String>();
        selectedAperture = new ArrayList<String>();
        selectedShutter = new ArrayList<String>();
        selectedIso = new ArrayList<String>();
    }

    public void clearAll(){
        selectedCameraModel.clear();
        selectedLensId.clear();
        selectedFocalLength.clear();
        selectedAperture.clear();
        selectedShutter.clear();
        selectedIso.clear();
    }

    /**
     * @return the selectedCameraModel
     */
    public ArrayList<String> getSelectedCameraModel() {
        return selectedCameraModel;
    }

    /**
     * @param selectedCameraModel the selectedCameraModel to set
     */
    public void setSelectedCameraModel(ArrayList<String> selectedCameraModel) {
        this.selectedCameraModel = selectedCameraModel;
    }

    /**
     * @return the selectedLensId
     */
    public ArrayList<String> getSelectedLensId() {
        return selectedLensId;
    }

    /**
     * @param selectedLensId the selectedLensId to set
     */
    public void setSelectedLensId(ArrayList<String> selectedLensId) {
        this.selectedLensId = selectedLensId;
    }

    /**
     * @return the selectedFocalLength
     */
    public ArrayList<String> getSelectedFocalLength() {
        return selectedFocalLength;
    }

    /**
     * @param selectedFocalLength the selectedFocalLength to set
     */
    public void setSelectedFocalLength(ArrayList<String> selectedFocalLength) {
        this.selectedFocalLength = selectedFocalLength;
    }

    /**
     * @return the selectedAperture
     */
    public ArrayList<String> getSelectedAperture() {
        return selectedAperture;
    }

    /**
     * @param selectedAperture the selectedAperture to set
     */
    public void setSelectedAperture(ArrayList<String> selectedAperture) {
        this.selectedAperture = selectedAperture;
    }

    /**
     * @return the selectedShutter
     */
    public ArrayList<String> getSelectedShutter() {
        return selectedShutter;
    }

    /**
     * @param selectedShutter the selectedShutter to set
     */
    public void setSelectedShutter(ArrayList<String> selectedShutter) {
        this.selectedShutter = selectedShutter;
    }

    /**
     * @return the selectedIso
     */
    public ArrayList<String> getSelectedIso() {
        return selectedIso;
    }

    /**
     * @param selectedIso the selectedIso to set
     */
    public void setSelectedIso(ArrayList<String> selectedIso) {
        this.selectedIso = selectedIso;
    }

}