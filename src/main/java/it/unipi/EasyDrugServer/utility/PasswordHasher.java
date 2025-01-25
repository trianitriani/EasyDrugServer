package it.unipi.EasyDrugServer.utility;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

public class PasswordHasher {
    public static String hash(String plainPassword) {
        // Genera un hash della password con un salt incorporato
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        // Verifica se la password in chiaro corrisponde all'hash
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
