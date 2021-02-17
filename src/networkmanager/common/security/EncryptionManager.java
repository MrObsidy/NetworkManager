package networkmanager.common.security;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {
	private PublicKey publicKey;
	private KeyAgreement agreement;
	byte[] encripter;
	
	private static final String ALGORITHM = "AES";
	
	private Key generateKey() {
		return new SecretKeySpec(encripter, ALGORITHM);
	}
	
	public EncryptionManager() throws NoSuchAlgorithmException, InvalidKeyException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(128);
			
		KeyPair keyPair = generator.generateKeyPair();
		publicKey = keyPair.getPublic();
			
		agreement = KeyAgreement.getInstance("ECDH");
		agreement.init(keyPair.getPrivate());
	}
	
	public void setReceiver(PublicKey key) {
		try {
			agreement.doPhase(key, true);
			encripter = agreement.generateSecret();
		} catch(InvalidKeyException e) {
			e.printStackTrace();
		}
	}
	
	public void setReceiverFromString(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] bytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		KeyFactory factory = KeyFactory.getInstance("EC");
		this.setReceiver(factory.generatePublic(spec));
	}
	
	public PublicKey getPublicKey() {
		return this.publicKey;
	}
	
	public String getPublicKeyAsString() {
		return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
	}
	
	public String encrypt(String input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Key key = this.generateKey();
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] pre = cipher.doFinal(input.getBytes());
		return Base64.getEncoder().encodeToString(pre);
	}
	
	public String decrypt(String input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Key key = this.generateKey();
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] decodedBase = Base64.getDecoder().decode(input);
		return new String(cipher.doFinal(decodedBase));
	}
}
