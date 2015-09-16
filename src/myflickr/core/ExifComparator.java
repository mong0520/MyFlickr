/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import java.util.Comparator;
import myflickr.util.AsciiToDigit;

public class ExifComparator implements Comparator<String>{

        public int compare(String s1, String s2) {
            if(s1.equalsIgnoreCase(Comm.NO_DATA)){s1="-1";}
            if(s2.equalsIgnoreCase(Comm.NO_DATA)){s2="-1";}
            String normalizedd_s1 = s1.replaceAll("[^0-9\\.\\-]", "");        // f/4.0 -> 4.0,  35mm -> 35
            String normalizedd_s2 = s2.replaceAll("[^0-9\\.\\-]", "");
            //System.out.println("normalizedd_s1= "+normalizedd_s1);
            //System.out.println("normalizedd_s2= "+normalizedd_s2);
            Float f1 = AsciiToDigit.atof(normalizedd_s1);
            Float f2 = AsciiToDigit.atof(normalizedd_s2);
            return f1.compareTo(f2);
        }
}