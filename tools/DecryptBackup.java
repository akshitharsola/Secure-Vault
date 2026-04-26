import java.io.*;
import java.nio.file.*;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;

/**
 * SecureVault Backup Decryption Utility
 * Decrypts SecureVault backup files and extracts password data
 */
public class DecryptBackup {

    // Encryption constants (must match BackupEncryption.kt)
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 32;
    private static final int ITERATION_COUNT = 100000;

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, ALGORITHM);
    }

    private static String decrypt(String encryptedData, String password) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        if (combined.length < SALT_LENGTH + IV_LENGTH) {
            throw new Exception("Invalid encrypted data format");
        }

        // Extract salt, IV, and encrypted data
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - SALT_LENGTH - IV_LENGTH];

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encrypted, 0, encrypted.length);

        // Derive key from password using same salt
        SecretKeySpec key = deriveKey(password, salt);

        // Decrypt the data
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] decryptedData = cipher.doFinal(encrypted);

        return new String(decryptedData, "UTF-8");
    }

    public static void main(String[] args) {
        String backupFile = args.length > 0 ? args[0] :
            "/Users/akshitharsola/Documents/SecureAttend/SecureVault-Android-master/app/SecureVault_Backup_2026-01-05_18-41-22.backup";
        String password = args.length > 1 ? args[1] : "147zaq";
        String outputFile = args.length > 2 ? args[2] : "decrypted_passwords.txt";

        System.out.println("=".repeat(70));
        System.out.println("SecureVault Backup Decryption Script");
        System.out.println("=".repeat(70));
        System.out.println("Backup file: " + backupFile);
        System.out.println("Output file: " + outputFile);
        System.out.println();

        try {
            // Read backup file
            System.out.println("[1/4] Reading backup file...");
            String backupContent = Files.readString(Paths.get(backupFile));
            System.out.println("✓ Backup file read successfully (" + backupContent.length() + " bytes)");

            // Parse JSON to extract encrypted data
            System.out.println("\n[2/4] Parsing backup JSON structure...");

            String encryptedData;
            if (backupContent.trim().startsWith("{")) {
                // JSON format - extract "data" or "f" field using proper JSON parsing
                // Look for the pattern "data": "..." or "f": "..."
                String pattern = backupContent.contains("\"data\"") ? "\"data\"" : "\"f\"";

                int fieldStart = backupContent.indexOf(pattern);
                if (fieldStart == -1) {
                    throw new Exception("No 'data' or 'f' field found in backup JSON");
                }

                // Find the start of the value (after ": ")
                int valueStart = backupContent.indexOf("\"", fieldStart + pattern.length()) + 1;

                // Find the end of the value, handling escaped quotes and unicode escapes
                StringBuilder sb = new StringBuilder();
                boolean inEscape = false;
                for (int i = valueStart; i < backupContent.length(); i++) {
                    char c = backupContent.charAt(i);

                    if (inEscape) {
                        // Handle escape sequences
                        if (c == 'u') {
                            // Unicode escape like \u003d
                            String unicode = backupContent.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(unicode, 16));
                            i += 4; // Skip the next 4 characters
                        } else if (c == 'n') {
                            sb.append('\n');
                        } else if (c == 'r') {
                            sb.append('\r');
                        } else if (c == 't') {
                            sb.append('\t');
                        } else if (c == '\\') {
                            sb.append('\\');
                        } else if (c == '"') {
                            sb.append('"');
                        } else {
                            sb.append(c);
                        }
                        inEscape = false;
                    } else if (c == '\\') {
                        inEscape = true;
                    } else if (c == '"') {
                        // End of string value
                        break;
                    } else {
                        sb.append(c);
                    }
                }

                encryptedData = sb.toString();
                String formatType = pattern.equals("\"data\"") ? "new format" : "legacy obfuscated format";
                System.out.println("✓ Found " + pattern + " field (" + formatType + ") - " + encryptedData.length() + " bytes");
            } else {
                // Very old format - entire file is encrypted data
                System.out.println("✓ Detected very old backup format (raw encrypted data)");
                encryptedData = backupContent.trim();
            }

            // Decrypt the data
            System.out.println("\n[3/4] Decrypting password data...");
            String decryptedJson = decrypt(encryptedData, password);
            System.out.println("✓ Decryption successful!");

            // Count passwords
            int passwordCount = 0;
            int idx = 0;
            while ((idx = decryptedJson.indexOf("{", idx + 1)) != -1) {
                passwordCount++;
            }
            System.out.println("✓ Found approximately " + passwordCount + " password entries");

            // Write to output file
            System.out.println("\n[4/4] Writing decrypted data to " + outputFile + "...");
            StringBuilder output = new StringBuilder();
            output.append("=".repeat(70)).append("\n");
            output.append("SecureVault Decrypted Backup Data\n");
            output.append("=".repeat(70)).append("\n");
            output.append("Backup file: ").append(backupFile).append("\n");
            output.append("Decrypted at: ").append(LocalDateTime.now()).append("\n");
            output.append("Password count: ~").append(passwordCount).append("\n");
            output.append("=".repeat(70)).append("\n\n");
            output.append("RAW DECRYPTED JSON:\n");
            output.append("-".repeat(70)).append("\n");
            output.append(decryptedJson).append("\n");
            output.append("-".repeat(70)).append("\n");

            Files.writeString(Paths.get(outputFile), output.toString());
            System.out.println("✓ Data written successfully!");

            System.out.println("\n" + "=".repeat(70));
            System.out.println("SUCCESS! Decrypted data saved to: " + outputFile);
            System.out.println("=".repeat(70));

            // Print preview
            System.out.println("\nPreview of decrypted JSON:");
            System.out.println("-".repeat(70));
            String preview = decryptedJson.length() > 1000 ?
                decryptedJson.substring(0, 1000) + "\n..." :
                decryptedJson;
            System.out.println(preview);
            System.out.println("-".repeat(70));

        } catch (Exception e) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("=".repeat(70));
            e.printStackTrace();
            System.exit(1);
        }
    }
}
