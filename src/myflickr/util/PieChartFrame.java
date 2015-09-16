/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Mong
 */


import com.flickr4java.flickr.photos.Photo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JFrame;
import myflickr.core.Comm;
import myflickr.core.PhotoWrapper;
import myflickr.core.UserWrapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.util.Rotation;
import org.jfree.util.SortOrder;

/**
 * A simple demonstration application showing how to create a pie chart using data from a
 * {@link DefaultPieDataset}.
 *
 */
public class PieChartFrame extends JFrame {
    private ArrayList<PhotoWrapper> photoList;    
    /**
     * Creates a new demo.
     *
     * @param title  the frame title.
     */
    public PieChartFrame(final String title, Comm.SelectType type, ArrayList<PhotoWrapper> list) {
        super(title);
        //set data source
        this.photoList = list;        
        // create a dataset...
        final PieDataset dataset = createSampleDataset(type);
        // create the chart...
        final JFreeChart chart = createChart(dataset);

        // add the chart to a panel...
        final ChartPanel chartPanel = new ChartPanel(chart,true, true, true, true, true);


        chartPanel.setPreferredSize(new java.awt.Dimension(640, 640));
        setContentPane(chartPanel);
    }



    /**
     * Creates a sample dataset for the demo.
     *
     * @return A sample dataset.
     */
    private PieDataset createSampleDataset(Comm.SelectType type) {
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        final DefaultPieDataset result = new DefaultPieDataset();
        //System.out.println("[Debug]: photoList size in createSampleDataSet = "+photoList.size());
            for(PhotoWrapper p: this.photoList){
                String data = null;
                if(type == Comm.SelectType.CAMERA_MODEL){data = p.getMeta().getExifDataWrapper().getCameraModel();}
                if(type ==Comm.SelectType.LENS_ID){data = p.getMeta().getExifDataWrapper().getLensModel();}
                if(type ==Comm.SelectType.FOCAL_LENGTH){data = p.getMeta().getExifDataWrapper().getFocalLength();}
                if(type ==Comm.SelectType.SHUTTER_SPEED){data = p.getMeta().getExifDataWrapper().getExposure();}
                if(type ==Comm.SelectType.APERUTRE_VALUE){data = p.getMeta().getExifDataWrapper().getAperture();}
                if(type ==Comm.SelectType.ISO_SPEED){data = p.getMeta().getExifDataWrapper().getIsoSpeed();}
                if(data.equalsIgnoreCase(Comm.NO_DATA)){continue;}
                if(hashMap.get(data)==null){ hashMap.put(data,0);  }
                hashMap.put(data, hashMap.get(data)+1);
            }
            Set keySet = hashMap.keySet();
            Iterator iterator = keySet.iterator();
            while(iterator.hasNext()){
                String data = (String) iterator.next();
                result.setValue(data, new Double(hashMap.get(data)));
            }
            result.sortByValues(SortOrder.ASCENDING);
            return result;
    }


    // ****************************************************************************
    // * JFREECHART DEVELOPER GUIDE                                               *
    // * The JFreeChart Developer Guide, written by David Gilbert, is available   *
    // * to purchase from Object Refinery Limited:                                *
    // *                                                                          *
    // * http://www.object-refinery.com/jfreechart/guide.html                     *
    // *                                                                          *
    // * Sales are used to provide funding for the JFreeChart project - please    *
    // * support us so that we can continue developing free software.             *
    // ****************************************************************************

    /**
     * Creates a sample chart.
     *
     * @param dataset  the dataset.
     *
     * @return A chart.
     */
    private JFreeChart createChart(final PieDataset dataset) {

        final JFreeChart chart = ChartFactory.createPieChart(
            "My Statistic Data",  // chart title
            dataset,                // data
            true,                   // include legend
            true,
            true
        );

        final PiePlot plot = (PiePlot) chart.getPlot();
        plot.setStartAngle(90);
        plot.setDirection(Rotation.ANTICLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        plot.setNoDataMessage("無資料");
        plot.setCircular(true);
        //plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} = {2}"));
        return chart;

    }

    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {

        final PieChartFrame demo = new PieChartFrame("Pie Chart 3D Demo 1", null, null);
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }

}
