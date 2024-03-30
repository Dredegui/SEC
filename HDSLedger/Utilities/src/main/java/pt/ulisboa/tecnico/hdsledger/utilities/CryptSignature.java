package pt.ulisboa.tecnico.hdsledger.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CryptSignature {

    public CryptSignature() {
    }

    public static String loadPublicKey(String publicKeyPath) {
        try {
            // Read all bytes from the path
            byte[] keyBytes = Files.readAllBytes(Paths.get(publicKeyPath));
            // Convert to a string, assuming the key is encoded in a standard format
            // remove the header, footer and newlines from key
            String uKey = new String(keyBytes);
            uKey = uKey.replace("-----BEGIN PUBLIC KEY-----", "");
            uKey = uKey.replace("-----END PUBLIC KEY-----", "");
            uKey = uKey.replaceAll("\\s+", "");

            return uKey;
        } catch (IOException e) {
            e.printStackTrace();
            return ""; // Or handle error appropriately
        }
    }

    public static String hashPublicKey(String publicKeyString) {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);

        String hashBase64 = Base64.getEncoder().encodeToString(hash(publicKeyBytes));
        
        return hashBase64;
    }

    public static byte[] hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            byte[] hashBytes = digest.digest(data);

            return hashBytes;
                        
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // function that creates a MAC from data and a key
    public static byte[] createMAC(byte[] data, byte[] key) {
        byte[] mac = new byte[32];
        try {
            // Create a Mac object and initialize it with the key
            Mac sha256_HMAC = Mac.getInstance("SHA-256");
            SecretKeySpec secret_key = new SecretKeySpec(key, "SHA-256");
            sha256_HMAC.init(secret_key);
            // Update and sign the data
            mac = sha256_HMAC.doFinal(data);
            return mac;
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
            return null;
        }
    }

    // function that validates a MAC
    public static boolean validateMAC(byte[] data, byte[] mac, byte[] key) {
        try {
            // Create a Mac object and initialize it with the key
            Mac sha256_HMAC = Mac.getInstance("SHA-256");
            SecretKeySpec secret_key = new SecretKeySpec(key, "SHA-256");
            sha256_HMAC.init(secret_key);
            // Update and sign the data
            byte[] newMac = sha256_HMAC.doFinal(data);
            // Compare the two MACs
            return MessageDigest.isEqual(mac, newMac);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] getSecretKey(String senderId, String receiverId) {
        Path path = Paths.get("src/main/resources/secrets/secret_" + senderId + "_" + receiverId + ".key");
        if (!path.toFile().exists()) {
            path = Paths.get("src/main/resources/secrets/secret_" + receiverId + "_" + senderId + ".key");
        }
        try {
            byte[] keyBytes = Files.readAllBytes(path);
            return keyBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static PublicKey getPublicKey(String publicKeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(publicKeyPath));
            // remove the header, footer and newlines from key
            String uKey = new String(keyBytes);
            uKey = uKey.replace("-----BEGIN PUBLIC KEY-----", "");
            uKey = uKey.replace("-----END PUBLIC KEY-----", "");
            uKey = uKey.replaceAll("\\s+", "");
            keyBytes = Base64.getDecoder().decode(uKey);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
	}

    public static PrivateKey getPrivateKey(String privateKeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
            // remove the header, footer and newlines from key
            String rKey = new String(keyBytes);
            rKey = rKey.replace("-----BEGIN PRIVATE KEY-----", "");
            rKey = rKey.replace("-----END PRIVATE KEY-----", "");
            rKey = rKey.replaceAll("\\s+", "");
            keyBytes = Base64.getDecoder().decode(rKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
    }

    // Function that generates a key signature from a value
    public static byte[] sign(byte[] data, String privateKey) {
        byte[] signature = new byte[256];
        // current node private key
        PrivateKey privKey = getPrivateKey(privateKey);
        try {
            // Create a Signature object and initialize it with the private key
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privKey);
            // Update and sign the data
            rsa.update(data);
            signature = rsa.sign();
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
        }
        return signature;
    }

    // inverse of the sign function aka validate
    public static boolean validate(byte[] data, byte[] signature, String publicKey) {
        //extract public key from .key file
        PublicKey pubKey = getPublicKey(publicKey);
        // validate signature
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(pubKey);
            rsa.update(data);
            return rsa.verify(signature);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
            return false;
        }
    }


}
