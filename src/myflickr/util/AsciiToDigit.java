/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

/**
 *
 * @author neil
 */
public class AsciiToDigit {

 public static void main(String[] args) {  
  //int x = AsciiToDigit.atoi("124.568asdf121 sadfas.");
     int x = AsciiToDigit.atoi("Hi 6400");
     System.out.println("Conversion is: " + x);
 }

 public static int atoi(String number) {
        int val = 0;
        boolean bNeg = false;
        boolean bMeetNum = false;
        char tmp ;
        int flag = 0;
        number = number.trim();
        tmp = number.charAt(flag);
        if(tmp == '-'){bNeg = true; flag++;}
        if(tmp == '+'){bNeg = false; flag++;}
        for (int i = flag ; i<number.length();i++){
            try{
                int digit = Integer.parseInt(String.valueOf(number.charAt(i)));
                val = val*10 + digit;
                bMeetNum = true;
            }catch(NumberFormatException e){
                if(bMeetNum){
                    break;
                }else{
                    continue;
                }
            }
        }
        if(bNeg)
            return val*-1;
        else
            return val;
    }
 public static float atof(String number){
        float val = (float)0.0;
        boolean bNeg = false;
        char tmp ;
        int flag = 0;
        boolean floatFound = false;
        double floatPos = 1;

        number = number.trim();
        tmp = number.charAt(flag);
        if(tmp == '-'){bNeg = true; flag++;}
        if(tmp == '+'){bNeg = false; flag++;}
        for (int i = flag ; i<number.length();i++){
            try{
                int digit = 0;
                if(!floatFound){
                    digit = Integer.parseInt(String.valueOf(number.charAt(i)));
                    val = val*10 + digit;
                }else{
                    digit = Integer.parseInt(String.valueOf(number.charAt(i)));
                    val = (float) ( val + digit * 0.1 * floatPos);
                    floatPos = floatPos/10;
                }

            }catch(NumberFormatException e){
                if(number.charAt(i) == '.'){
                    floatFound = true;
                }
                else{
                    break;
                }
            }
        }
        if(bNeg)
            return val*-1;
        else
            return val;
 }

}
