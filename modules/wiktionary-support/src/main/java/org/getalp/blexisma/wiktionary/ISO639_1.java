package org.getalp.blexisma.wiktionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ISO639_1 {
    public class Lang {
        String a3b, a3t, a2, fr, en, epo;
    }
    private final static String linePatternString =
        "^(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)$";
    private final static String epolinePatternString =
        "^(.*?)\\|(.*?)$";
    private final static Pattern linePattern = Pattern.compile(linePatternString);
    private final static Pattern epolinePattern = Pattern.compile(epolinePatternString);
       
    //public static InputStream fis = ISO639_1.class.getClassLoader().getResourceAsStream("ISO639.data");
    public static ISO639_1 sharedInstance = new ISO639_1();
    private Map<String, Lang> langMap = new HashMap<String,Lang>();

    private ISO639_1() {
        InputStream fis = null;
        try {
            fis = this.getClass().getResourceAsStream("ISO639.data");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            
            Matcher matcher = linePattern.matcher(new String(""));

            String s = br.readLine();
            while (s != null) {
                matcher.reset(s);
                if (matcher.find()) {
                    // System.out.println(matcher.group(5));
                    // a3b, a3t, a2, en, fr
                    Lang l = new Lang();
                    l.a3b = matcher.group(1);
                    l.a3t = matcher.group(2);
                    l.a2  = matcher.group(3);
                    l.en  = matcher.group(4);
                    l.fr  = matcher.group(5);
                    
                    langMap.put(l.a3b, l);
                    langMap.put(l.a3t, l);
                    langMap.put(l.a2, l);
                    
                } else {
                    System.out.println("Unrecognized line:" + s);                    
                }
                s = br.readLine();
            }
            
            
        } catch (UnsupportedEncodingException e) {
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    
                }
        }
        fis = null;
        try {
            fis = this.getClass().getResourceAsStream("ISO639-eponym.data");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            
            Matcher matcher = epolinePattern.matcher(new String(""));

            String s = br.readLine();
            while (s != null) {
                matcher.reset(s);
                if (matcher.find()) {
                    // System.out.println(matcher.group(5));
                    // a3b, a3t, a2, en, fr
                    Lang l = langMap.get(matcher.group(1));
                    if (l != null)
                        l.epo = matcher.group(2);
                    // else 
                        // System.out.println("Unknown language code: " + matcher.group(1));        
                } else {
                    System.out.println("Unrecognized line:" + s);                    
                }
                s = br.readLine();
            }
        } catch (UnsupportedEncodingException e) {
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    
                }
        }
    }
 
    public String getLanguageNameInFrench(String langcode) {
        return langMap.get(langcode).fr;
    }
    
    public String getLanguageNameInEnglish(String langcode) {
        return langMap.get(langcode).en;
    }
    
    public String getBib3Code(String langcode) {
        return langMap.get(langcode).a3b;
    }
    
    public String getTerm3Code(String langcode) {
        Lang l = langMap.get(langcode);
        return (l.a3t == null) ? l.a3b : l.a3t;
    }
    
    public String getTerm2Code(String langcode) {
        return langMap.get(langcode).a2;
    }
    
    public Lang getLang(String langcode) {
        return langMap.get(langcode);
    }
    
    public static void main(String ars[]) {
        ;
    }
}
