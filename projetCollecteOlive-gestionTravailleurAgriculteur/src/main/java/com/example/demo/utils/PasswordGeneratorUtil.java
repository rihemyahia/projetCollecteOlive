package com.example.demo.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PasswordGeneratorUtil {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS    = "0123456789";
    private static final String SPECIAL   = "!@#$%^&*()-_=+[]{}";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int    LENGTH    = 12;

    public String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Guarantee at least one character from each category
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS   .charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL  .charAt(random.nextInt(SPECIAL.length())));

        // Fill the rest randomly
        for (int i = 4; i < LENGTH; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        // Shuffle to avoid predictable positions
        List<Character> chars = new ArrayList<>();
        for (char c : password.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars, random);

        StringBuilder result = new StringBuilder();
        chars.forEach(result::append);
        return result.toString();
    }
}