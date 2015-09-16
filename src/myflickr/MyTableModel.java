/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author neil
 */
public class MyTableModel extends DefaultTableModel{
    
    Class[] types = new Class [] {
                Object.class, Integer.class, String.class, String.class, String.class, Integer.class, Float.class, String.class, Integer.class, String.class, ImageIcon.class /*, String.class*/
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false /*, false*/
            };

    public MyTableModel() {
        super(new Object [][] {}, new String [] {"hidden entity", "編號", "照片名稱", "相機型號", "鏡頭ID", "實際焦長 (mm)", "光圈值", "快門", "ISO值", "拍攝時間", "縮圖"/*, "標籤"*/, }
        );                        
    }
    public Class getColumnClass(int columnIndex) {
        return types [columnIndex];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit [columnIndex];
    }
}
