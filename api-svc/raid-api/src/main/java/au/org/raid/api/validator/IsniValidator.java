package au.org.raid.api.validator;


import org.springframework.stereotype.Component;

public class IsniValidator {

    public boolean validate(String isni) {

        final var id = isni.substring(isni.lastIndexOf("/") + 1);

        if (id == null || id.length() != 16) {
            return false;
        }

        // Extract the first 15 digits and the check character
        String digits = id.substring(0, 15);
        char checkChar = id.charAt(15);

        // Verify that the first 15 characters are all digits
        if (!digits.matches("\\d{15}")) {
            return false;
        }

        // Verify that the check character is either a digit or 'X'
        if (!Character.isDigit(checkChar) && checkChar != 'X') {
            return false;
        }

        // Calculate the check character using MOD 11-2 algorithm
        char calculatedCheck = calculateCheckCharacter(digits);

        return checkChar == calculatedCheck;
    }

    private char calculateCheckCharacter(String digits) {
        int sum = 0;

        // Process each digit from left to right
        for (int i = 0; i < digits.length(); i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            sum = (sum + digit) * 2;
        }

        // Calculate check value: the value needed so that (sum + checkValue) ≡ 1 (mod 11)
        int remainder = sum % 11;
        int checkValue = (12 - remainder) % 11;

        // If check value is 10, return 'X', otherwise return the digit
        return (checkValue == 10) ? 'X' : (char) ('0' + checkValue);
    }
}