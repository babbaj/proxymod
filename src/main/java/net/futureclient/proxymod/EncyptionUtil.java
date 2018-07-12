package net.futureclient.proxymod;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public final class EncyptionUtil {
    private EncyptionUtil() {}

    public static SecretKey decodeSecretKey(byte[] encodedKey)
    {
        return new SecretKeySpec(encodedKey, "AES");
    }
}
