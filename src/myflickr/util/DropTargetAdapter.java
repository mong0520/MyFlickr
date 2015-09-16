/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import javax.swing.*;

/**
 *
 * @author neil
 */
public class DropTargetAdapter implements DropTargetListener{

//private JLabel lb;
private JList displayList;

public DropTargetAdapter(JList list){
    //this.lb = lb;
    this.displayList = list;    
}

public void dragEnter(DropTargetDragEvent dtde) {
    //System.out.println("Drag Enter");
  }

  public void dragExit(DropTargetEvent dte) {
    //System.out.println("Drag Exit");
  }

  public void dragOver(DropTargetDragEvent dtde) {
    //System.out.println("Drag Over");
  }

  public void dropActionChanged(DropTargetDragEvent dtde) {
    //System.out.println("Drop Action Changed");
  }

  public void drop(DropTargetDropEvent dtde) {
    DefaultListModel model = (DefaultListModel) displayList.getModel();
        try {
          // Ok, get the dropped object and try to figure out what it is
          Transferable tr = dtde.getTransferable();
          DataFlavor[] flavors = tr.getTransferDataFlavors();
          File file2upload = null;
          for (int i = 0; i < flavors.length; i++) {
            //System.out.println("Possible flavor: " + flavors[i].getMimeType());
            // Check for file lists specifically
            if (flavors[i].isFlavorJavaFileListType())
            {
                // Great!  Accept copy drops...
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                //lb.setText("Successful file list drop.\n\n");

                // And add the list of file names to our text area
                java.util.List list = (java.util.List)tr.getTransferData(flavors[i]);
                for (int j = 0; j < list.size(); j++) {
                //lb.setText(lb.getText() + list.get(j) + "\n");
                    file2upload = new File(list.get(j).toString());
                    if(!model.contains(file2upload))
                    {                        
                        int pos = model.getSize();                        
                        model.add(pos, file2upload);
                    }
                }
            // If we made it this far, everything worked.
            dtde.dropComplete(true);
            return;
            }
          }
          // Hmm, the user must not have dropped a file list
          System.out.println("Drop failed: " + dtde);
          dtde.rejectDrop();
        } catch (Exception e) {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

}
