package test;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class DropTest2 extends JFrame implements DropTargetListener {

  DropTarget dt;
  //JTextArea ta;
  JLabel lb;

  public DropTest2() {
    super("Drop Test");
    setSize(300,300);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    getContentPane().add(
        new JLabel("Drop a list from your file chooser here:"),
  BorderLayout.NORTH);
    lb = new JLabel();
    lb.setBackground(Color.white);
    getContentPane().add(lb, BorderLayout.CENTER);

    // Set up our text area to recieve drops...
    // This class will handle drop events
    dt = new DropTarget(lb, this);
    setVisible(true);
  }

  public void dragEnter(DropTargetDragEvent dtde) {
    System.out.println("Drag Enter");
  }

  public void dragExit(DropTargetEvent dte) {
    System.out.println("Drag Exit");
  }

  public void dragOver(DropTargetDragEvent dtde) {
    System.out.println("Drag Over");
  }

  public void dropActionChanged(DropTargetDragEvent dtde) {
    System.out.println("Drop Action Changed");
  }

  public void drop(DropTargetDropEvent dtde) {
    try {
      // Ok, get the dropped object and try to figure out what it is
      Transferable tr = dtde.getTransferable();
      DataFlavor[] flavors = tr.getTransferDataFlavors();
      for (int i = 0; i < flavors.length; i++) {
  System.out.println("Possible flavor: " + flavors[i].getMimeType());
  // Check for file lists specifically
  if (flavors[i].isFlavorJavaFileListType()) {
    // Great!  Accept copy drops...
    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
    lb.setText("Successful file list drop.\n\n");

    // And add the list of file names to our text area
    java.util.List list = (java.util.List)tr.getTransferData(flavors[i]);
    for (int j = 0; j < list.size(); j++) {
      lb.setText(lb.getText() + list.get(j) + "\n");
    }

    // If we made it this far, everything worked.
    dtde.dropComplete(true);
    return;
  }
  // Ok, is it another Java object?
  else if (flavors[i].isFlavorSerializedObjectType()) {
    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
    lb.setText("Successful text drop.\n\n");
    Object o = tr.getTransferData(flavors[i]);
    lb.setText(lb.getText() + "Object: " + o);
    dtde.dropComplete(true);
    return;
  }
  // How about an input stream?
  else if (flavors[i].isRepresentationClassInputStream()) {
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

  public static void main(String args[]) {
    new DropTest2();
  }
}
