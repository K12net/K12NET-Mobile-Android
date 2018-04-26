package com.k12nt.k12netframe.utils.helper;

/**
 * Created by tarikcanturk on 10/01/2018.
 */

public class K12NetHelper {

    public static int getInt(String valueStr, int defaultVal) {
        int convertedValue = defaultVal;
        try {
            convertedValue = Integer.valueOf(valueStr);
        } catch (Exception ex) {

        }
        return convertedValue;
    }

    public static int findPattermCount(String word, String pattern){
        int count = word.length() - word.replace(pattern, "").length();
        return count;
    }
}
