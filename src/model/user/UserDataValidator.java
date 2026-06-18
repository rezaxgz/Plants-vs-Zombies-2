package model.user;

import java.util.ArrayList;
import java.util.List;

public class UserDataValidator {
    public static boolean isValidUsername(String username) {
        return username.matches("[-a-zA-Z0-9]+");
    }

    public static List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        String specials = "!#$%^&*()=+{}[]|/\\\\:;'\",<>?";

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c))
                hasLower = true;
            else if (Character.isUpperCase(c))
                hasUpper = true;
            else if (Character.isDigit(c))
                hasDigit = true;
            else if (specials.indexOf(c) >= 0)
                hasSpecial = true;
        }

        if (password.length() < 8)
            errors.add("Length must be at least 8.");

        if (!hasLower)
            errors.add("Missing lowercase letter.");

        if (!hasUpper)
            errors.add("Missing uppercase letter.");

        if (!hasDigit)
            errors.add("Missing digit.");

        if (!hasSpecial)
            errors.add("Missing special character.");

        return errors;
    }

    public static boolean isValidNickname(String nickname) {
        return nickname.length() <= 30 && nickname.length() > 2;
    }

    public static String validateEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex == -1 || email.indexOf('@', atIndex) != -1) {
            return "email must contain one and only one @";
        }

        String username = email.substring(0, atIndex);
        String usernameErr = validateUsernameInEmail(username);
        if (usernameErr != null) {
            return usernameErr;
        }

        String domain = email.substring(atIndex + 1);
        String domainErr = validateDomainInEmail(domain);
        if (domainErr != null) {
            return domainErr;
        }

        String invalidChars = ":!#$%^&*()=+}{[]|/\\:;'\",><";
        for (char c : invalidChars.toCharArray()) {
            if (email.indexOf(c) != -1) {
                return "email cannot contain the charachter '" + c + "'";
            }
        }
        return null;
    }

    private static String validateUsernameInEmail(String s) {
        if (s == null || s.isEmpty()) {
            return "username in email cannot be empty";
        }
        int n = s.length();
        if (!Character.isLetterOrDigit(s.charAt(0)) || !Character.isLetterOrDigit(s.charAt(n - 1))) {
            return "username in email must start and end with a digit or number";
        }
        boolean prevDot = false;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_') {
                return "username in email must only contain letters, digits, dots, -, _";
            }
            if (c == '.') {
                if (prevDot) {
                    return "username in email cannot have consecutive dots";
                }
                prevDot = true;
            } else {
                prevDot = false;
            }
        }
        return null;
    }

    private static String validateDomainInEmail(String s) {
        if (s == null || s.isEmpty()) {
            return "domain in email cannot be empty";
        }

        int n = s.length();

        if (!Character.isLetterOrDigit(s.charAt(0)) || !Character.isLetterOrDigit(s.charAt(n - 1))) {
            return "domain in email must start and end with a digit or number";
        }

        boolean hasDot = false;
        int lastDotIndex = -1;

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);

            if (!Character.isLetterOrDigit(c) && c != '-' && c != '.') {
                return "domain in email can only contain letters, digits and hyphen";
            }

            if (c == '.') {
                hasDot = true;
                lastDotIndex = i;
            }
        }

        if (!hasDot) {
            return "domain in email must contain at least one dot";
        }

        String tld = s.substring(lastDotIndex + 1);

        if (tld.length() < 2) {
            return "domain extension must be at least 2 charachters long";
        }
        for (char c : tld.toCharArray()) {
            if (!Character.isLetter(c)) {
                return "domain extenstion ca only contain letters";
            }
        }

        return null;
    }
}
