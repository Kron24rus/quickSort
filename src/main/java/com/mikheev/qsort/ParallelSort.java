package com.mikheev.qsort;

import mpi.Intracomm;
import mpi.MPI;
import mpi.Status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ParallelSort {
    public static void main(String... args) {
        MPI.Init(args);
        long start = 0;
        final Intracomm comm = MPI.COMM_WORLD;
        final int rank = comm.Rank();
        final int procNum = comm.Size();
        double[] inputArray = {};
        double[] procMasNew;
        double[] procMasSend;
        int[] procArrayNewSize = new int[1];
        int[] procArraySendSize = new int[1];
        int[] arrayChunkSize = new int[1];
        int[] arraySize = new int[1];
        int[] arrayChunkSizes;

        //Считывание данных из файла
        if (rank == 0) {
            try {
                Scanner sc = new Scanner(new File(args[3]));
                arraySize[0] = sc.nextInt();
                inputArray = new double[arraySize[0]];
                for (int i = 0; i < arraySize[0]; i++) {
                    inputArray[i] = sc.nextDouble();
                }
            } catch (FileNotFoundException e) {
                return;
            }
            arrayChunkSize[0] = arraySize[0] / procNum;
        }

        //Определение размеров чанков
        comm.Bcast(arraySize, 0, 1, MPI.INT, 0);
        comm.Bcast(arrayChunkSize, 0, 1, MPI.INT, 0);
        int modulo = arraySize[0] - arrayChunkSize[0] * procNum;
        arrayChunkSizes = new int[procNum];
        for (int i = 0; i < procNum; i++) {
            if (i < modulo) {
                arrayChunkSizes[i] = arrayChunkSize[0] + 1;
            } else {
                arrayChunkSizes[i] = arrayChunkSize[0];
            }
        }

        //Рассчитать смещение для рассылки частей по процессорам (Scatterv)
        int[] displs = new int[procNum];
        displs[0] = 0;
        for (int i = 1; i < procNum; i++) {
            displs[i] = displs[i - 1] + arrayChunkSizes[i - 1];
        }
        int chunkSize = arrayChunkSizes[rank];
        if (rank != 0) {
            inputArray = new double[arraySize[0]];
        }
        double[] local = new double[arrayChunkSizes[rank]];
        comm.Scatterv(inputArray, 0, arrayChunkSizes, displs, MPI.DOUBLE, local, 0, arrayChunkSizes[rank], MPI.DOUBLE, 0);

        //Подсчет количества итераций
        int iterations = 0;
        while ((1 << iterations) != procNum && (1 << iterations) < procNum) {
            iterations++;
        }

        if (rank == 0) start = System.nanoTime();
        int currentBitDigit, pairProcessRank, color;
        List<Intracomm> communicators = new ArrayList<>();
        double[] pivot = new double[1];
        for (int i = iterations - 1; i >= 0; i--) {
            currentBitDigit = 1 << i;
            pairProcessRank = rank | currentBitDigit;
            if (rank == pairProcessRank)
                pairProcessRank -= currentBitDigit;

            //рассылаем средний элемент по гиперкубам
            int power = iterations - i - 1;
            color = (int) (rank / (procNum / (Math.pow(2, power))));
            if (i == iterations - 1) {
                communicators.add(power, comm.Split(color, rank));
            } else {
                int localRank = communicators.get(power - 1).Rank();
                communicators.add(power, communicators.get(power - 1).Split(color, localRank));
            }
            Intracomm localCommunicator = communicators.get(power);
            if (localCommunicator.Rank() == 0) {
                pivot[0] = local[0];
            }
            localCommunicator.Bcast(pivot, 0, 1, MPI.DOUBLE, 0);

            procMasNew = new double[chunkSize];
            procMasSend = new double[chunkSize];
            procArrayNewSize[0] = 0;
            procArraySendSize[0] = 0;

            if (rank < pairProcessRank) {
                lessThan(local, chunkSize, procMasNew, procArrayNewSize, procMasSend, procArraySendSize, pivot[0]);
                comm.Isend(procMasSend,0, procArraySendSize[0], MPI.DOUBLE, pairProcessRank, rank);
                Status status = comm.Probe(pairProcessRank, pairProcessRank);
                double[] newarr = new double[status.count];
                comm.Recv(newarr, 0, status.count, MPI.DOUBLE, pairProcessRank, pairProcessRank);
                local = mergeArrays(Arrays.copyOfRange(procMasNew, 0, procArrayNewSize[0]), newarr);
                chunkSize = procArrayNewSize[0] + newarr.length;
            } else {
                greaterThan(local, chunkSize, procMasNew, procArrayNewSize, procMasSend, procArraySendSize, pivot[0]);
                comm.Isend(procMasSend, 0, procArraySendSize[0], MPI.DOUBLE, pairProcessRank, rank);
                Status status = comm.Probe(pairProcessRank, pairProcessRank);
                double[] newarr = new double[status.count];
                comm.Recv(newarr, 0, status.count, MPI.DOUBLE, pairProcessRank, pairProcessRank);
                local = mergeArrays(Arrays.copyOfRange(procMasNew, 0, procArrayNewSize[0]), newarr);
                chunkSize = procArrayNewSize[0] + newarr.length;
            }
        }
        //Сортируем и затем собираем куски
        if (local.length > 1) Arrays.sort(local);
        int[] counts = new int[procNum];
        int[] localCount = {local.length};
        comm.Gather(localCount, 0, 1, MPI.INT, counts, 0, 1, MPI.INT, 0);

        if (rank == 0) {
            displs = new int[procNum];
            displs[0] = 0;
            for (int i = 1; i < procNum; i++) {
                displs[i] = displs[i - 1] + counts[i - 1];
            }
        }
        comm.Gatherv(local, 0, local.length, MPI.DOUBLE, inputArray, 0, counts, displs, MPI.DOUBLE, 0);
        long finish = System.nanoTime();

        if(rank == 0) {
            try {
                PrintWriter writer = new PrintWriter(arraySize[0] + "-" + comm.Size() + "-parallel.out");
                DecimalFormat df = new DecimalFormat("0.00000");
                writer.write(df.format((double) (finish - start) / 1000000000));
                writer.write("\n");
//                for (int i = 0; i < inputArray.length; i++) {
//                    writer.write(inputArray[i] + "\n");
//                }
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        comm.Barrier();
        MPI.Finalize();
    }

    private static double[] mergeArrays(double[] a, double[] b){
        double[] result = new double[a.length + b.length];
        int indexA = 0;
        int indexB = 0;
        for (int i = 0; i < result.length; i++) {
            if (indexA < a.length) {
                result[i] = a[indexA];
                indexA++;
            } else {
                result[i] = b[indexB];
                indexB++;
            }
        }
        return result;
    }

    private static void lessThan(double[] array, int arraySize, double[] newArray, int[] newArraySize, double[] arraySend
            , int[] arraySendSize, double pivot) {

        for (int t = 0; t < arraySize; ++t) {
            if (array[t] <= pivot) {
                newArray[newArraySize[0]] = array[t];
                ++newArraySize[0];
            }
            else {
                arraySend[arraySendSize[0]] = array[t];
                ++arraySendSize[0];
            }
        }
    }

    private static void greaterThan(double[] mas, int masSize, double[] masNew, int masNewSize[], double[] masSend
            , int masSendSize[], double pivot) {

        for (int t = 0; t < masSize; ++t) {
            if (mas[t] > pivot) {
                masNew[masNewSize[0]] = mas[t];
                ++masNewSize[0];
            }
            else {
                masSend[masSendSize[0]] = mas[t];
                ++masSendSize[0];
            }
        }
    }

}