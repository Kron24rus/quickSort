package com.mikheev.jacobi;

import mpi.MPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;

public class Jacobi2 {

    private static int ROOT = 0;

    public static void main(String[] args) {
        MPI.Init(args);
        int procRank = MPI.COMM_WORLD.Rank();
        int worldSize = MPI.COMM_WORLD.Size();

        double[] a = new double[0];
        double[] b = new double[0];
        double[] local_a;
        double[] local_b;
        double[] local_x;
        double[] eps = new double[1];
        int[] matrixSize = new int[1];
        int chunk;

        if (procRank == ROOT) {
            try {
                Scanner scanner = new Scanner(new File("temp/A8.txt"));
                matrixSize[0] = scanner.nextInt();

                a = new double[matrixSize[0] * matrixSize[0]];
                b = new double[matrixSize[0]];

                for (int i = 0; i < matrixSize[0]; i++) {
                    for (int j = 0; j < matrixSize[0]; j++) {
                        a[i * matrixSize[0] + j] = scanner.nextDouble();
                    }
                    b[i] = scanner.nextDouble();
                }
                eps[0] = scanner.nextDouble();
            } catch (FileNotFoundException e) {
                System.out.println("File not found!");
                System.exit(-1);
            }
        }

        MPI.COMM_WORLD.Bcast(matrixSize, 0, 1, MPI.INT, 0);
        MPI.COMM_WORLD.Bcast(eps, 0, 1, MPI.DOUBLE, 0);
        chunk = matrixSize[0] / worldSize;
        System.out.println("Proc: " + procRank + " size and eps broadcasted: " + matrixSize[0] + " " + eps[0]);
        if (procRank == 0)
        System.out.println(a.length + "AAAAAAAAAAAAAA");
        local_a = new double[matrixSize[0] * chunk];
        local_b = new double[chunk];
        local_x = new double[chunk];

        System.out.println("Rroc: "+ procRank + " " + a.length + " " + local_a.length);
        if (procRank == ROOT) {
            for (int i = 0; i < worldSize; i++) {
                double[] tempBuffer = new double[matrixSize[0] * chunk];
                for (int j = 0; j < matrixSize[0] * chunk; j++) {
                    tempBuffer[j] = a[i * matrixSize[0] + j];
                }
                System.out.println("FIRST!");
                MPI.COMM_WORLD.Send(tempBuffer, 0, matrixSize[0] * chunk, MPI.DOUBLE, i, 0);
            }

        }
        MPI.COMM_WORLD.Recv(local_a, 0, matrixSize[0] * chunk, MPI.DOUBLE, ROOT, 0);
        System.out.println("Proc: " + procRank + " recv " + local_a.length);
       // MPI.COMM_WORLD.Scatter(a, 0, matrixSize[0] * chunk, MPI.DOUBLE, local_a, 0, matrixSize[0] * chunk, MPI.DOUBLE, 0);

        MPI.Finalize();
    }

}
