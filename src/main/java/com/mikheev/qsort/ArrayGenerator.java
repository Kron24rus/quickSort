package com.mikheev.qsort;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ArrayGenerator {
    public static void main(String[] args) throws FileNotFoundException {
        //int[] sizes = new int[]{10000, 100000, 1000000, 10000000, 25000000};
        int[] sizes = new int[]{24000000};
        int lower = -10000000;
        int upper = 10000000;

        for (int size : sizes) {
            PrintWriter writer = new PrintWriter(size + ".in");
            writer.write(size + "\n");
            for (int j = 0; j < size; j++) {
                double value = Math.random() * (upper - lower) + lower;
                writer.write(value + " ");
            }
            writer.close();
        }
    }
}
