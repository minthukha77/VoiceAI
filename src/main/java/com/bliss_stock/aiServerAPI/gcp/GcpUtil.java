package com.bliss_stock.aiServerAPI.gcp;

public class GcpUtil {
    protected static final String[] SPECIAL_CHARS= {"\t", "\b", "\n", "\r", "\f", "\'", "\"", "\\"};
    static String replaceSpecialChars(String result){
        System.out.println("result: " + result);
        String replacer = " ";
        String tmpResult;
        for (var c: SPECIAL_CHARS){
            if(result.contains(c)){
                tmpResult = result.replace(c, replacer);
                result = tmpResult;
            }
        }
        System.out.println("result modified: " + result);
        return result;
    }
}
