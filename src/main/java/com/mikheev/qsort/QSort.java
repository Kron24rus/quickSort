package com.mikheev.qsort;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by ��������� on 29.07.2015.
 */
public class QSort {

    protected static Random rand = new Random(47);

    public static void main(String[] args) throws FileNotFoundException {
        String arraySize = args[0];
        Scanner sc = new Scanner(new File(arraySize + ".in"));
        PrintWriter pw = new PrintWriter(new File(arraySize + "-consistent.out"));

        int N = sc.nextInt();
        double m[] = new double[N];
        int l = 0, r = N - 1;
        for (int i = 0; i < N; i++) {
            m[i] = sc.nextDouble();
        }

        long start = System.nanoTime();
        qsort(m, l, r);
        long finish = System.nanoTime();
        DecimalFormat df = new DecimalFormat("0.00000");
        pw.write(df.format((double)(finish - start) / 1000000000));
        pw.write("\n");
        for (int i = 0; i < N; i++) {
            pw.write(m[i] + "\n");
         //   System.out.print(m[i] + " ");
        }

        pw.close();
    }

    public static void qsort(double[] m, int l, int r) {

        int i = l, j = r;
        double med = m[rand.nextInt(r - l) + l];

        while (true) {
            while (m[i] < med) i++;
            while (m[j] > med) j--;


            if (i <= j) {
                double tmp = m[i];
                m[i] = m[j];
                m[j] = tmp;
                i++; j--;
        //        System.out.println(i + " " + j);
            }

            if (i > j) break;
        }
//        for (int f = 0; f < m.length; f++) {
//            System.out.print(m[f] + " ");
//        }
//        System.out.println("");

        if (l < j) qsort(m, l, j);
        if (i < r) qsort(m, i, r);
    }
}
