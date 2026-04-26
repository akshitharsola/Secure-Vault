#!/usr/bin/env python3
"""
SecureVault Backup Decryption Script
Decrypts SecureVault backup files and extracts password data
"""

import sys
import json
import base64
import re
from datetime import datetime
from Crypto.Cipher import AES
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Hash import SHA256

# Encryption constants (must match BackupEncryption.kt)
KEY_LENGTH = 32  # 256 bits / 8
IV_LENGTH = 16
SALT_LENGTH = 32
ITERATION_COUNT = 100000

def derive_key(password, salt):
    """Derives encryption key from password using PBKDF2"""
    return PBKDF2(
        password.encode('utf-8'),
        salt,
        dkLen=KEY_LENGTH,
        count=ITERATION_COUNT,
        hmac_hash_module=SHA256
    )

def decrypt(encrypted_data_b64, password):
    """Decrypts data using password-based encryption"""
    # Decode from Base64
    combined = base64.b64decode(encrypted_data_b64)

    if len(combined) < SALT_LENGTH + IV_LENGTH:
        raise ValueError("Invalid encrypted data format")

    # Extract salt, IV, and encrypted data
    salt = combined[0:SALT_LENGTH]
    iv = combined[SALT_LENGTH:SALT_LENGTH + IV_LENGTH]
    encrypted = combined[SALT_LENGTH + IV_LENGTH:]

    # Derive key from password using same salt
    key = derive_key(password, salt)

    # Decrypt the data
    cipher = AES.new(key, AES.MODE_CBC, iv)
    decrypted_data = cipher.decrypt(encrypted)

    # Remove PKCS5 padding
    padding_length = decrypted_data[-1]
    decrypted_data = decrypted_data[:-padding_length]

    return decrypted_data.decode('utf-8')

def main():
    backup_file = sys.argv[1] if len(sys.argv) > 1 else "/Users/akshitharsola/Documents/SecureAttend/SecureVault-Android-master/app/SecureVault_Backup_2026-01-05_18-41-22.backup"
    password = sys.argv[2] if len(sys.argv) > 2 else "147zaq"
    output_file = sys.argv[3] if len(sys.argv) > 3 else "decrypted_passwords.txt"

    print("=" * 70)
    print("SecureVault Backup Decryption Script")
    print("=" * 70)
    print(f"Backup file: {backup_file}")
    print(f"Output file: {output_file}")
    print()

    try:
        # Read backup file
        print("[1/4] Reading backup file...")
        with open(backup_file, 'r') as f:
            backup_content = f.read()
        print(f"✓ Backup file read successfully ({len(backup_content)} bytes)")

        # Parse JSON to extract encrypted data
        print("\n[2/4] Parsing backup JSON structure...")

        # Try to parse as JSON first
        try:
            backup_json = json.loads(backup_content)

            # Check for different format keys
            if 'data' in backup_json:
                encrypted_data = backup_json['data']
                print(f"✓ Found 'data' field (new format) - {len(encrypted_data)} bytes")
            elif 'f' in backup_json:
                encrypted_data = backup_json['f']
                print(f"✓ Found 'f' field (legacy obfuscated format) - {len(encrypted_data)} bytes")
            else:
                raise ValueError("No 'data' or 'f' field found in backup JSON")

        except json.JSONDecodeError:
            # Very old format - entire file is encrypted data
            print("✓ Detected very old backup format (raw encrypted data)")
            encrypted_data = backup_content.strip()

        # Decrypt the data
        print("\n[3/4] Decrypting password data...")
        decrypted_json = decrypt(encrypted_data, password)
        print("✓ Decryption successful!")

        # Parse decrypted JSON to pretty print it
        try:
            passwords = json.loads(decrypted_json)
            pretty_json = json.dumps(passwords, indent=2)
            password_count = len(passwords) if isinstance(passwords, list) else "unknown"
            print(f"✓ Found {password_count} password entries")
        except:
            pretty_json = decrypted_json
            password_count = "unknown"

        # Write to output file
        print(f"\n[4/4] Writing decrypted data to {output_file}...")
        with open(output_file, 'w') as f:
            f.write("=" * 70 + "\n")
            f.write("SecureVault Decrypted Backup Data\n")
            f.write("=" * 70 + "\n")
            f.write(f"Backup file: {backup_file}\n")
            f.write(f"Decrypted at: {datetime.now()}\n")
            f.write(f"Password count: {password_count}\n")
            f.write("=" * 70 + "\n\n")
            f.write("RAW DECRYPTED JSON:\n")
            f.write("-" * 70 + "\n")
            f.write(pretty_json)
            f.write("\n" + "-" * 70 + "\n")

        print("✓ Data written successfully!")

        print("\n" + "=" * 70)
        print(f"SUCCESS! Decrypted data saved to: {output_file}")
        print("=" * 70)

        # Print preview
        print("\nPreview of decrypted JSON:")
        print("-" * 70)
        preview = pretty_json[:1000]
        print(preview + ("..." if len(pretty_json) > 1000 else ""))
        print("-" * 70)

    except Exception as e:
        print("\n" + "=" * 70)
        print(f"ERROR: {str(e)}")
        print("=" * 70)
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
