package com.mikheev.jacobi;

import mpi.MPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Scanner;

public class Solution {
    public static void main(String[] args) throws FileNotFoundException {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int worldSize = MPI.COMM_WORLD.Size();

        int matrixSize = 0;
        double eps = 0;
        double[][] matrix = null;

        System.out.println("Hello world from <"+me+"> from <"+worldSize);
        // Для считывания воспользуемся классом Scanner
        Scanner scanner = new Scanner(new File("input.txt"));
        // Для того, чтобы целая часть вещественного числа
        // отделялась от дробной точкой, а не запятой,
        // необходимо установить русский Locale
        scanner.useLocale(new Locale("Russian"));

        // Для вывода - классом PrintWriter
        PrintWriter printWriter = new PrintWriter(System.out);

        // Считываем размер вводимой матрицы

        int size;
        matrixSize = scanner.nextInt();

        // Будем хранить матрицу в векторе, состоящем из
        // векторов вещественных чисел
        matrix = new double[matrixSize][matrixSize + 1];

        // Матрица будет иметь размер (size) x (size + 1),
        // c учетом столбца свободных членов
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize + 1; j++) {
                matrix[i][j] = scanner.nextDouble();
            }
        }
        // Считываем необходимую точность решения
        eps = scanner.nextDouble();

        // Введем вектор значений неизвестных на предыдущей итерации,
        // размер которого равен числу строк в матрице, т.е. size,
        // причем согласно методу изначально заполняем его нулями
        double[] previousVariableValues = new double[matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            previousVariableValues[i] = 0.0;
        }
        // Будем выполнять итерационный процесс до тех пор,
        // пока не будет достигнута необходимая точность
        while (true) {
            // Введем вектор значений неизвестных на текущем шаге
            double[] currentVariableValues = new double[matrixSize];
            // Посчитаем значения неизвестных на текущей итерации
            // в соответствии с теоретическими формулами
            for (int i = 0; i < matrixSize; i++) {
                // Инициализируем i-ую неизвестную значением
                // свободного члена i-ой строки матрицы
                currentVariableValues[i] = matrix[i][matrixSize];
                // Вычитаем сумму по всем отличным от i-ой неизвестным
                for (int j = 0; j < matrixSize; j++) {
                    if (i != j) {
                        currentVariableValues[i] -= matrix[i][j] * previousVariableValues[j];
                    }
                }
                // Делим на коэффициент при i-ой неизвестной
                currentVariableValues[i] /= matrix[i][i];
            }
            // Посчитаем текущую погрешность относительно предыдущей итерации
            double error = 0.0;
            for (int i = 0; i < matrixSize; i++) {
                error += Math.abs(currentVariableValues[i] - previousVariableValues[i]);
            }
            // Если необходимая точность достигнута, то завершаем процесс
            if (error < eps) {
                break;
            }
            // Переходим к следующей итерации, так
            // что текущие значения неизвестных
            // становятся значениями на предыдущей итерации
            previousVariableValues = currentVariableValues;
            for (int i = 0; i < matrixSize; i++) {
                printWriter.print(previousVariableValues[i] + " ");
            }
            printWriter.print("\n");
        }
        // Выводим найденные значения неизвестных
        printWriter.print("Final:\n");
        for (int i = 0; i < matrixSize; i++) {
            printWriter.print(previousVariableValues[i] + " ");
        }
        // После выполнения программы необходимо закрыть
        // потоки ввода и вывода
        scanner.close();
        printWriter.close();
        MPI.Finalize();
    }
}