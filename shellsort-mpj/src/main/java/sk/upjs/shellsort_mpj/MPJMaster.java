package sk.upjs.shellsort_mpj;

import java.util.Arrays;

import mpi.MPI;

public class MPJMaster implements Runnable {

	int processCount = MPI.COMM_WORLD.Size();
	int myid = MPI.COMM_WORLD.Rank();
	
	public void run() {
		
		// pole, ktore sa bude sortovat
		// bud vlozime vlastne alebo nechame vygenerovat nahodne s danou dlzkou
		int[] arrayToSort = Utils.generateRandomArray(1000);
		Utils.log(myid, "array to sort: " + Arrays.toString(arrayToSort));
		
		// vypocitanie offsetu pre scatter aby bol schopny poslat, kazdemu procesu rovnaky kusok pola (chunk)
		// (ak je velkost pola delitelna poctom procesov tak je rovny nule)
		// a vypocitanie velkosti kusku pola, ktory dostane kazdy proces
		int offset = arrayToSort.length % processCount;
		int chunkSize = arrayToSort.length/processCount;
		
		// preposlanie informaciu o velkosti pola pre sortovanie, aby si tiez vedeli vypocitat offset a velkost kusku
		MPI.COMM_WORLD.Bcast(new int[] {arrayToSort.length}, 0, 1, MPI.INT, 0);
		
		// velkost pola, ktore mu usortit master je vacsie o pocet prvkov, ktore sme museli pri scatter ignorovat
		// aby sa kazdemu procesu dokazal rozdelit rovnako velky kusok
		int[] recvbuf = new int[chunkSize + offset];
		
		// ak je offset nenulovy, teda vynechali sme nejake prvky tak ich nakopirujeme na zaciatok pola,
		// ktore budeme sortovat
		if (offset != 0) {
			for(int i = 0; i < offset; i++) {
				recvbuf[i] = arrayToSort[i];
			}
		}
		
		// rozposlanie rovnako velkych kuskov pola kazdemu procesu
		MPI.COMM_WORLD.Scatter(arrayToSort, offset, chunkSize, MPI.INT, recvbuf, offset, chunkSize, MPI.INT, 0);
		
		// zvolenie postupnosti medzier, ktore sa pri sortovani pouziju
		// je mozne vygenerovanie niektorych znamych postupnopsti medzier
		// pomocou statickych metod v triede Utils,
		// no je mozne aj zadat vlastne pole postupnosti
		int[] gaps;
		gaps = Utils.getShellGaps(arrayToSort.length);
//		gaps = Utils.getFrankLazarusGaps(arrayToSort.length);
//		gaps = Utils.getHibbardGaps(arrayToSort.length);
//		gaps = Utils.getPapernovStasevichGaps(arrayToSort.length);
//		gaps = Utils.getSedgewickGaps(arrayToSort.length);
//		gaps = Utils.getCiuraGaps();
		
		Utils.log(myid, "gaps = " + Arrays.toString(gaps));
		
		// vsetetkym procesom sa posle najprv velkost pola medzier
		// a nasledne samotne pole medzier, ktore procesy pouziju pri sortovani
		MPI.COMM_WORLD.Bcast(new int[] {gaps.length}, 0, 1, MPI.INT, 0);
		MPI.COMM_WORLD.Bcast(gaps, 0, gaps.length, MPI.INT, 0);
		
		// vykonanie sortovania na danom poli pomocou danej postupnosti medzier
		Utils.shellSort(recvbuf, gaps);
		
		// paralelne spajanie usortovanych poli medzi procesmi tak, 
		// ze na konci je u mastra cele usortene pole
		int[] sorted = Utils.parallelMerge(processCount, myid, recvbuf);
		
		// vypisanie usorteneho pola a overenie ze je cele usortene pomocou statickej metody isSorted
		Utils.log(myid, "result = " + Arrays.toString(sorted));
		Utils.log(myid, "isSorted = " + Utils.isSorted(sorted));
	}

}
