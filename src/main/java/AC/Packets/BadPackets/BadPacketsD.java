package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class BadPacketsD {

    // List of extremely offensive and harmful terms
    private static final List<String> BLACKLISTED_WORDS = Arrays.asList(
            // Racial slurs
            "nigger", "chink", "paki", "gypsy",
            "sandnigger", "negro", "niggor", "Nigga",

            // Sexually explicit and derogatory terms
            "rape", "molest", "incest", "pedophile", "bestiality", "gangbang",
            "orgy", "fuckable", "cumshot", "deepthroat", "blowjob", "cum", "anal",
            "spunk", "assmunch", "sex", "masturbation", "porn", "pornhub", "sexting",
            "jizz", "climax", "nude", "blowjob", "cocksucker",

            // Offensive terms related to disabilities
            "cripple", "retarded", "mongoloid", "spazz", "handicapped", "special needs","goon",
            "gooning",

            // Gender and sexuality related offensive terms
            "dyke", "butch", "queer", "gay", "lesbian", "transgender", "bi-curious",
            "sex-change", "hermaphrodite", "transphobic", "homophobic", "genderqueer",
            "drag queen", "twink", "Trans", "tranny",

            // Hate speech and extremist terms
            "terrorist", "nazi", "kkk", "isis", "alqaeda", "hitler", "bomb", "suicide",
            "shooting", "holocaust", "torture",

            // Offensive terms for women and sexuality
            "whore", "slut", "cunt", "bimbo", "prostitute", "faggot", "bastard",

            // Other derogatory terms
            "bitchass", "suckmydick", "dickhead", "motherfucker"
    );

    // Method to check if the message contains blacklisted words
    public boolean isValid(String message) {
        if (message == null || message.isEmpty()) {
            return true; // An empty message is considered valid
        }
        // Convert message to lowercase for case-insensitive comparison
        String lowerCaseMessage = message.toLowerCase();
        // Check if any blacklisted word is present in the message
        for (String blacklistedWord : BLACKLISTED_WORDS) {
            if (lowerCaseMessage.contains(blacklistedWord)) {
                return false; // Found a blacklisted word, so the packet is invalid
            }
        }
        return true; // No blacklisted word found
    }
}