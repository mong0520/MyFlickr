/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myflickr;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 *
 * @author neil
 */
public class CustomRenderer implements TableCellRenderer{
        TableCellRenderer render;
        Border b;
        public CustomRenderer(TableCellRenderer r, Color top, Color left,Color bottom, Color right){
            render = r;

            //It looks funky to have a different color on each side - but this is what you asked
            //You can comment out borders if you want too. (example try commenting out top and left borders)
            b = BorderFactory.createCompoundBorder();
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(2,0,0,0,top));
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(0,2,0,0,left));
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(0,0,2,0,bottom));
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(0,0,0,2,right));
        }
        
        public CustomRenderer(TableCellRenderer r, Color c){
            render = r;

            //It looks funky to have a different color on each side - but this is what you asked
            //You can comment out borders if you want too. (example try commenting out top and left borders)
            //b = BorderFactory.createCompoundBorder();
            //b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(1,1,1,1,c));            
            b = BorderFactory.createLineBorder(c, 1);            
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            JComponent result = (JComponent)render.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            result.setBorder(b);
            return result;
        }

    }