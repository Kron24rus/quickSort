import subprocess
from multiprocessing import cpu_count
import os
import time

# FOR WINDOWS
# executable = "mpiexec -n %d python jacobi.py A%s.txt B%s.txt %.0e"
executable = "/home/kron/projects/mpj/mpj-v0_44/lib/starter.jar runtime.starter.MPJRun -jar target/classes/com/mikheev/jacobi/Jacobi -np %d temp/A%d.txt temp/%d.txt temp/%d-res.txt"
cores = (1, 2, 4)
matrix_sizes = (300, 2000)
accuracies = (1e-3,)


DEVNULL = open(os.devnull, "w")

for core in cores:
    print("%d cores..." % core, end="")
    for size in matrix_sizes:
        print("%d " % size, end="")
        for eps in accuracies:
            exec = executable % (core, size, size, eps)
            process = subprocess.Popen(exec, stdout=DEVNULL, shell=True)
            process.communicate()
    print()
