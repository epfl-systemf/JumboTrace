package com.epfl.systemf.jumbotrace;

public final class Formatting {

    private Formatting(){
        throw new AssertionError("not instantiable");
    }

    public static String lastNameOnly(String namesSeq){
        if (namesSeq == null){
            return "?";
        }
        var idx = namesSeq.length()-1;
        while (idx >= 0){
            char c = namesSeq.charAt(idx);
            if (c == '/' || c == '\\' || c == '.'){
                return namesSeq.substring(idx+1);
            }
            idx--;
        }
        return namesSeq;
    }

}
