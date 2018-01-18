package com.mikheev.jacobi;

import mpi.MPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Jacobi {

    public static void main(String[] args) {
        Scanner scanner;
        Scanner vectorScanner;
        PrintWriter writer;
        if (args.length >= 6) {
            try {
                scanner = new Scanner(new File(args[3]));
                vectorScanner = new Scanner(new File(args[4]));
                writer = new PrintWriter(new File(args[5]));
            } catch (FileNotFoundException e) {
                System.err.println("FileNotFoundException!");
                return;
            }
        } else {
            return;
        }
        double[] pA = new double[0];
        double[] pB = new double[0];
        double[] pX = new double[0];
        double[] local_a;
        double[] local_b;
        double[] local_x;
        double[] maxNorm = new double[1];
        double[] norm = new double[1];
        int[] n = new int[1];
        int chunk, procNum, procRank;
        double eps = 0.0000001;
        double start = 0, finish = 0;

        MPI.Init(args);
        procRank = MPI.COMM_WORLD.Rank();
        procNum = MPI.COMM_WORLD.Size();

        if (procRank == 0) {
            double st = System.nanoTime();
            n[0] = scanner.nextInt();
            int nplus = scanner.nextInt();
            int bsize = vectorScanner.nextInt();

            pA = new double[n[0] * n[0]];
            pB = new double[n[0]];

            for (int i = 0; i < n[0]; i++) {
                for (int j = 0; j < n[0]; j++) {
                    pA[i * n[0] + j] = scanner.nextDouble();
                }
                pB[i] = scanner.nextDouble();
            }
            pX = new double[n[0]];
            for (int i = 0; i < n[0]; i++) {
                pX[i] = vectorScanner.nextDouble();
            }
            scanner.close();
            vectorScanner.close();
            double stop = System.nanoTime();
            System.out.println("Read time: = " + (stop - st) / 1000000000 + " sec.");
        }

        MPI.COMM_WORLD.Bcast(n, 0, 1, MPI.INT, 0);

        if (procRank != 0) {
            pA = new double[n[0] * n[0]];
            pB = new double[n[0]];
            pX = new double[n[0]];
        }

        chunk = n[0] / procNum;
        local_a = new double[chunk*n[0]];
        local_b = new double[chunk];
        local_x = new double[chunk];

        MPI.COMM_WORLD.Bcast(pX, 0, n[0], MPI.DOUBLE, 0);
        MPI.COMM_WORLD.Scatter(pA,0, chunk * n[0], MPI.DOUBLE, local_a, 0, chunk * n[0],MPI.DOUBLE,0);
        MPI.COMM_WORLD.Scatter(pB,0, chunk, MPI.DOUBLE,local_b,0,chunk,MPI.DOUBLE,0);
        if (procRank == 0) start = System.nanoTime();
        do {
            for (int i = 0; i < chunk; i++) {
                local_x[i] = -local_b[i];
                for (int j = 0; j < n[0]; j++) {
                    if (procRank * chunk + i != j) {
                        local_x[i] += local_a[i * n[0] + j] * pX[j];
                    }
                }
                local_x[i] /= -local_a[procRank * chunk + i + i * n[0]];
            }
            norm[0] = Math.abs(pX[procRank * chunk] - local_x[0]);
            for (int i = 0; i < chunk; i++) {
                if (Math.abs(pX[procRank * chunk + i] - local_x[i]) > norm[0]) {
                    norm[0] = Math.abs(pX[procRank * chunk + i] - local_x[i]);
                }
            }
            MPI.COMM_WORLD.Reduce(norm, 0, maxNorm, 0, 1, MPI.DOUBLE, MPI.MAX, 0);
            MPI.COMM_WORLD.Bcast(maxNorm, 0, 1, MPI.DOUBLE, 0);
            MPI.COMM_WORLD.Allgather(local_x, 0, chunk, MPI.DOUBLE, pX, 0, chunk, MPI.DOUBLE);
        } while (maxNorm[0] > eps);

        if (procRank == 0) {
            finish = System.nanoTime();
            writer.write(String.valueOf((finish - start) / 1000000000));
            System.out.println("Calculate time: = " + (finish - start) / 1000000000 + " sec.");
//            for (int i = 0; i < pX.length; i++) {
//                if (i % 4 == 0 && i > 0) writer.write("\n");
//                writer.write(pX[i] + "\t");
//            }
            double[] resultVector = new double[n[0]];
            for (int i = 0; i < n[0]; i++) {
                for (int j = 0; j < n[0]; j++) {
                    resultVector[i] += pA[j + i * n[0]] * pX[j];
                }
            }

            for (int i = 0; i < resultVector.length; i++) {
                System.out.println(resultVector[i] -  pB[i]);
            }

            writer.close();
        }
        MPI.Finalize();
    }
}