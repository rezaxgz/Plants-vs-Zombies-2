package io.github.some_example_name.model.user;

import java.util.ArrayList;
import java.util.List;

public final class UserDataValidator {
    private static final String PASSWORD_SPECIALS = "!#$%^&*()=+{}[]|/\\:;'\",<>?";

    private UserDataValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[-a-zA-Z0-9]+");
    }

    public static List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null) {
            errors.add("Password cannot be empty.");
            return errors;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasInvalidCharacter = false;

        for (char character : password.toCharArray()) {
            if (isAsciiLower(character)) {
                hasLower = true;
            } else if (isAsciiUpper(character)) {
                hasUpper = true;
            } else if (isAsciiDigit(character)) {
                hasDigit = true;
            } else if (PASSWORD_SPECIALS.indexOf(character) >= 0) {
                hasSpecial = true;
            } else {
                hasInvalidCharacter = true;
            }
        }

        if (password.length() < 8) {
            errors.add("Length must be at least 8.");
        }
        if (!hasLower) {
            errors.add("Missing lowercase letter.");
        }
        if (!hasUpper) {
            errors.add("Missing uppercase letter.");
        }
        if (!hasDigit) {
            errors.add("Missing digit.");
        }
        if (!hasSpecial) {
            errors.add("Missing special character.");
        }
        if (hasInvalidCharacter) {
            errors.add("Password contains an invalid character.");
        }
        return errors;
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null
                && nickname.length() >= 3
                && nickname.length() <= 30;
    }

    public static String validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "email cannot be empty";
        }

        int firstAt = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        if (firstAt <= 0 || firstAt != lastAt
                || firstAt == email.length() - 1) {
            return "email must contain one and only one @";
        }

        String localPart = email.substring(0, firstAt);
        String localError = validateLocalPart(localPart);
        if (localError != null) {
            return localError;
        }

        String domain = email.substring(firstAt + 1);
        return validateDomain(domain);
    }

    private static String validateLocalPart(String localPart) {
        if (!isAsciiLetterOrDigit(localPart.charAt(0))
                || !isAsciiLetterOrDigit(
                        localPart.charAt(localPart.length() - 1))) {
            return "email username must start and end with a letter or digit";
        }
        if (localPart.contains("..")) {
            return "email username cannot contain consecutive dots";
        }
        for (char character : localPart.toCharArray()) {
            boolean allowed = isAsciiLetterOrDigit(character)
                    || character == '.' || character == '-'
                    || character == '_';
            if (!allowed) {
                return "email username contains an invalid character";
            }
        }
        return null;
    }

    private static String validateDomain(String domain) {
        if (domain.contains("..")) {
            return "email domain cannot contain consecutive dots";
        }
        String[] labels = domain.split("\\.", -1);
        if (labels.length < 2) {
            return "email domain must contain at least one dot";
        }
        for (String label : labels) {
            String error = validateDomainLabel(label);
            if (error != null) {
                return error;
            }
        }

        String topLevelDomain = labels[labels.length - 1];
        if (topLevelDomain.length() < 2) {
            return "domain extension must be at least 2 characters long";
        }
        for (char character : topLevelDomain.toCharArray()) {
            if (!isAsciiLetter(character)) {
                return "domain extension can only contain English letters";
            }
        }
        return null;
    }

    private static String validateDomainLabel(String label) {
        if (label.isEmpty()) {
            return "email domain cannot contain empty sections";
        }
        if (!isAsciiLetterOrDigit(label.charAt(0))
                || !isAsciiLetterOrDigit(label.charAt(label.length() - 1))) {
            return "each email domain section must start and end with a letter or digit";
        }
        for (char character : label.toCharArray()) {
            if (!isAsciiLetterOrDigit(character) && character != '-') {
                return "email domain contains an invalid character";
            }
        }
        return null;
    }

    private static boolean isAsciiLetterOrDigit(char character) {
        return isAsciiLetter(character) || isAsciiDigit(character);
    }

    private static boolean isAsciiLetter(char character) {
        return isAsciiLower(character) || isAsciiUpper(character);
    }

    private static boolean isAsciiLower(char character) {
        return character >= 'a' && character <= 'z';
    }

    private static boolean isAsciiUpper(char character) {
        return character >= 'A' && character <= 'Z';
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
