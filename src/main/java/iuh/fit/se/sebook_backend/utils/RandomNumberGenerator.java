package iuh.fit.se.sebook_backend.utils;


import java.util.Random;

public class RandomNumberGenerator {
    public String generateNumber() {
        Random random = new Random();
        return String.valueOf(100_000 + random.nextInt(900_000));
    }
}