package hu.rycus.intellihome.util;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to create MD5 hashes.
 *
 * Created by Viktor Adam on 12/8/13.
 */
public class MD5Util {

    /** Converts the number to a two-charactered string. */
    private static String hex(int num) {
        return (num < 0x10 ? "0" : "") + Integer.toHexString(num);
    }

    /** Retruns the hex representation for the MD5 hash of the given string. */
    public static String toMD5(String source) {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(source.getBytes());
            byte[] digest = digester.digest();

            StringBuilder hexBuilder = new StringBuilder();
            for(byte d : digest) {
                hexBuilder.append(hex(d & 0xFF));
            }
            return hexBuilder.toString();
        } catch(NoSuchAlgorithmException nsaex) {
            Log.w("MD5", "Failed to get MD5 message digester", nsaex);
        }

        return null;
    }

}
