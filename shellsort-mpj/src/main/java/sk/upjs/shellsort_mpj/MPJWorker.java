package sk.upjs.shellsort_mpj;

import java.util.Arrays;

import mpi.MPI;

public class MPJWorker implements Runnable {

	int processCount = MPI.COMM_WORLD.Size();
	int myid = MPI.COMM_WORLD.Rank();
	
	public void run() {
		// prijatie informacie o velkosti celeho paralelne sortovaneho pola
		// a nasledne vypocitanie offsetu a velkosti kusku, ktory procesu prijde pomocou scatter
		int[] size = new int[1];
		MPI.COMM_WORLD.Bcast(size, 0, 1, MPI.INT, 0);
		int offset = size[0] % processCount; 
		int chunkSize = size[0] / processCount;
		
		// pomocou scatter proces prijme svoj prideleny kusok, ktory ma usortit
		int[] recvbuf = new int[chunkSize];
		MPI.COMM_WORLD.Scatter(new int[size[0]], offset, chunkSize, MPI.INT, recvbuf, 0, chunkSize, MPI.INT, 0);
//		Utils.log(myid, Arrays.toString(recvbuf));
		
		// od mastra dostane informaciu o velkosti pola medzier, ktore sa pouziju na sortovanie
		int[] gapsSize = new int[1];
		MPI.COMM_WORLD.Bcast(gapsSize, 0, 1, MPI.INT, 0);
		
		// prijatie samotneho pola s postupnostami medzier
		int[] gaps = new int[gapsSize[0]];
		MPI.COMM_WORLD.Bcast(gaps, 0, gaps.length, MPI.INT, 0);
		
		// usortenie priradeneho kusku
		Utils.shellSort(recvbuf, gaps);
//		Utils.log(myid, Arrays.toString(recvbuf));
		
		// paralelne spajanie usortovanych poli medzi procesmi,
		// kedze usortene pole skonci u mastra tak po ukonceni svojej ulohy
		// pri paralelnom spajani sa vykonavanie daneho workera konci
		Utils.parallelMerge(processCount, myid, recvbuf);
	}

}
