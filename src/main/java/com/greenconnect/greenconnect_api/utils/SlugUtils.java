package com.greenconnect.greenconnect_api.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class để tạo slug từ text.
 * Slug là chuỗi thân thiện với URL, được sử dụng cho SEO.
 */
public class SlugUtils {
    
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    
    /**
     * Tạo slug từ text input
     * 
     * @param input Text cần convert thành slug
     * @return Slug string
     */
    public static String createSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        String slug = input.trim().toLowerCase(Locale.ENGLISH);
        
        // Xử lý tiếng Việt
        slug = removeVietnameseAccents(slug);
        
        // Normalize unicode
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        
        // Replace whitespace với dashes
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        
        // Remove non-latin characters 
        slug = NON_LATIN.matcher(slug).replaceAll("");
        
        // Remove dashes từ đầu và cuối
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        
        // Replace multiple dashes với single dash
        slug = slug.replaceAll("-+", "-");
        
        return slug;
    }
    
    /**
     * Remove Vietnamese accents từ string
     */
    private static String removeVietnameseAccents(String str) {
        str = str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        str = str.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        str = str.replaceAll("[ìíịỉĩ]", "i");
        str = str.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        str = str.replaceAll("[ùúụủũưừứựửữ]", "u");
        str = str.replaceAll("[ỳýỵỷỹ]", "y");
        str = str.replaceAll("[đ]", "d");
        
        str = str.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        str = str.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        str = str.replaceAll("[ÌÍỊỈĨ]", "I");
        str = str.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        str = str.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        str = str.replaceAll("[ỲÝỴỶỸ]", "Y");
        str = str.replaceAll("[Đ]", "D");
        
        return str;
    }
}