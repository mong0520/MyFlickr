/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.table.*;

import jxl.*;
import jxl.write.*;
import myflickr.core.Comm;


/**
 *
 * @author neil
 */
public class ExcelExporter {


    public Comm.RetrunCode fillData(JTable table, File file) {

        try {
            WritableWorkbook workbook1 = Workbook.createWorkbook(file);
            WritableSheet sheet1 = workbook1.createSheet("Data", 0);
            TableModel model = table.getModel();

            for (int i = 0; i < model.getColumnCount(); i++) {
                Label column = new Label(i, 0, model.getColumnName(i));
                sheet1.addCell(column);
            }
            int j = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                for (j = 0; j < model.getColumnCount(); j++) {
                    Label row = new Label(j, i + 1,
                            model.getValueAt(i, j).toString());
                    sheet1.addCell(row);
                }
            }
            CellView firstColumnView = new CellView();            
            firstColumnView.setHidden(true);            
            sheet1.setColumnView(0, firstColumnView);
            workbook1.write();
            workbook1.close();
            return Comm.RetrunCode.SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
            return Comm.RetrunCode.ERROR;
        }
    }
}