package com.albakm.skillboxexamplechat;

import android.os.CpuUsageInfo;
import android.util.Base64;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    private static final String pass="медвежьяпипирочка";
    private static SecretKeySpec keySpec;

    //хэширование, односторонняя операция
    static {
        try {
            MessageDigest shaDiggets = MessageDigest.getInstance("SHA-256");
            byte[] bytes = pass.getBytes();
            shaDiggets.update(bytes);
            byte[] hash = shaDiggets.digest();
            keySpec = new SecretKeySpec(hash, "AES");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static String Encrypt(String unencryptedText){
        try {
            Cipher cipher = Cipher.getInstance("AES");//взяди объект шифра
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);//настроили шифр
            byte[] bytes=cipher.doFinal(unencryptedText.getBytes());//зашифровали строку в байты
            return Base64.encodeToString(bytes,Base64.DEFAULT);//зашифровали в base64
        }
        catch (Exception ex){
            ex.printStackTrace();
            return unencryptedText;
        }
    }

    public static String Decrypt(String encryptedText){
        try {
            byte[] ciphered=Base64.decode(encryptedText,Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES");//взяди объект шифра
            cipher.init(Cipher.DECRYPT_MODE, keySpec);//настроили шифр
            byte[] bytes=cipher.doFinal(ciphered);//дешифровали строку в байты
            return new String(bytes, "UTF-8");
        }
        catch (Exception ex){
            ex.printStackTrace();
            return encryptedText;
        }
    }
}
