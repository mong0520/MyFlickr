/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
/**
 *
 * @author neil
 */

public class FileRenderer extends DefaultListCellRenderer {


    @Override
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

        Component c = super.getListCellRendererComponent(
            list,value,index,isSelected,cellHasFocus);
        JLabel l = (JLabel)c;
        File f = (File)value;
        l.setText(f.getName());
        if(f.exists()){
            l.setIcon(FileSystemView.getFileSystemView().getSystemIcon(f));
        }
        return l;
    }
}