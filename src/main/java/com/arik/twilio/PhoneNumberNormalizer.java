package com.arik.twilio;

/**
 * Class for normalizing human-generated into a common, consistent format
 */
public class PhoneNumberNormalizer {

    /**
     * Humans have many ways of representing phone numbers, Twilio only has one. This method finds the one.
     * *
     *
     * @param phoneNumber A human-created phone number
     * @return A Twilio-friendly phone number
     */
    public static String normalizePhoneNumber(final String phoneNumber) {

        // next, let's normalize the phone number representation
        String normalizedPhoneNumber = phoneNumber;

        // if we have a +, it's url-decoded as ' ', which could be problematic
        if (normalizedPhoneNumber.startsWith(" ")) {
            normalizedPhoneNumber = "+" + normalizedPhoneNumber.trim();
        }

        // in some places intl numbers start with 00, but Twilio requires a +
        if (normalizedPhoneNumber.startsWith("00")) {
            normalizedPhoneNumber = "+" + normalizedPhoneNumber.substring(2);
        }

        return normalizedPhoneNumber;

    }

}
