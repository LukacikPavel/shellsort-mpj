package sk.upjs.shellsort_mpj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mpi.MPI;

public class Utils {
	
	// metoda Shell sort, ktora pouzije pri sorteni zadane pole medzier
	// vytvorena pomocou pseudokodu na stranke wikipedie 
	// odkaz: https://en.wikipedia.org/wiki/Shellsort
	public static void shellSort(int[] array, int[] gaps) {

		for (int gap : gaps) {
			for (int i = gap; i < array.length; i++) {
				int temp = array[i];
				int j;
				for (j = i; j >= gap && array[j - gap] > temp; j -= gap) {
					array[j] = array[j - gap];
				}
				array[j] = temp;
			}
		}
	}

	
	// metoda na vytvorenie medzier, ktore pouziva povodny Shell sort
	// autor: Shell, 1959
	public static int[] getShellGaps(int arrayLength) {
		List<Integer> gaps = new ArrayList<Integer>();
		for (int i = arrayLength / 2; i > 0; i /= 2) {
			gaps.add(i);
		}
		return gaps.stream().mapToInt(i -> i).toArray();
	}

	// metoda na vytvorenie medzier
	// autor: Frank & Lazarus, 1960
	public static int[] getFrankLazarusGaps(int arrayLength) {
		List<Integer> gaps = new ArrayList<Integer>();
		int divisor = 4;
		int gap = 2 * (arrayLength / divisor) + 1;
		while (gap != 1) {
			gaps.add(gap);
			divisor *= 2;
			gap = 2 * (arrayLength / divisor) + 1;
		}
		gaps.add(1);
		return gaps.stream().mapToInt(i -> i).toArray();
	}

	// metoda na vytvorenie medzier
	// autor: Hibbard, 1963
	public static int[] getHibbardGaps(int arrayLength) {
		List<Integer> gaps = new ArrayList<Integer>();
		int k = 1;
		int gap = (int) (Math.pow(2, k) - 1);
		while (gap <= arrayLength) {
			gaps.add(gap);
			k++;
			gap = (int) (Math.pow(2, k) - 1);
		}
		
		int[] result = new int[gaps.size()];
		int j = gaps.size() - 1;
		for (int i = 0; i < result.length; i++) {
			result[i] = gaps.get(j);
			j--;
		}
		
		return result;
	}
	
	// metoda na vytvorenie medzier
	// autor: Papernov & Stasevich, 1965
	public static int[] getPapernovStasevichGaps(int arrayLength) {
		List<Integer> gaps = new ArrayList<Integer>();
		gaps.add(1);
		int k = 1;
		int gap = (int) (Math.pow(2, k) + 1);
		while (gap <= arrayLength) {
			gaps.add(gap);
			k++;
			gap = (int) (Math.pow(2, k) + 1);
		}
		
		int[] result = new int[gaps.size()];
		int j = gaps.size() - 1;
		for (int i = 0; i < result.length; i++) {
			result[i] = gaps.get(j);
			j--;
		}
		
		return result;
	}
	
	// metoda na vytvorenie medzier
	// autor: Sedgewick, 1982
	public static int[] getSedgewickGaps(int arrayLength) {
		List<Integer> gaps = new ArrayList<Integer>();
		gaps.add(1);
		int k = 1;
		int gap = (int) (Math.pow(2, k) + 1);
		while (gap < arrayLength) {
			gaps.add(gap);
			k++;
			gap = (int) (Math.pow(4, k) + 3*Math.pow(2, k-1) + 1);
		}
		
		int[] result = new int[gaps.size()];
		int j = gaps.size() - 1;
		for (int i = 0; i < result.length; i++) {
			result[i] = gaps.get(j);
			j--;
		}
		
		return result;
	}

	// metoda na vytvorenie medzier
	// experimentalne zistene
	// autor: Ciura, 2001
	public static int[] getCiuraGaps() {
		return new int[] { 1750, 701, 301, 132, 57, 23, 10, 4, 1 };
	}

	// paralelne spajanie usporiadanych poli medzi procesmi
	// diagram priebehu spacania je v prilozenom pdf
	// ak je procesom worker tak navratova hodnota je null
	// ak je procesom master (id = 0) tak je navratovou hodnotou cele usortene pole
	public static int[] parallelMerge(int activeProcessCount, int processId, int[] chunk) {
		int half = (int) Math.ceil((activeProcessCount / 2d));
		while (activeProcessCount != 1) {
			if (processId < half && processId + half < activeProcessCount) {
				int[] size = new int[1];
				MPI.COMM_WORLD.Recv(size, 0, 1, MPI.INT, processId + half, 0);
				int[] recvbuf = new int[size[0]];
				MPI.COMM_WORLD.Recv(recvbuf, 0, recvbuf.length, MPI.INT, processId + half, 0);
				chunk = merge(chunk, recvbuf);

			} else {
				MPI.COMM_WORLD.Send(new int[] { chunk.length }, 0, 1, MPI.INT, processId - half, 0);
				MPI.COMM_WORLD.Send(chunk, 0, chunk.length, MPI.INT, processId - half, 0);
				return null;
			}

			half = (int) Math.ceil((half / 2d));
			activeProcessCount = (int) Math.ceil((activeProcessCount / 2d));
		}
		return chunk;
	}

	// spojenie dvoch usporiadanych poli do usporiadaneho pola
	private static int[] merge(int[] arr1, int[] arr2) {
		int[] merged = new int[arr1.length + arr2.length];
		int i, j, k;
		i = j = k = 0;
		while (i < arr1.length && j < arr2.length) {
			if (arr1[i] <= arr2[j]) {
				merged[k] = arr1[i];
				i++;
			} else {
				merged[k] = arr2[j];
				j++;
			}
			k++;
		}

		while (i < arr1.length) {
			merged[k] = arr1[i];
			i++;
			k++;
		}

		while (j < arr2.length) {
			merged[k] = arr2[j];
			j++;
			k++;
		}
		return merged;
	}

	// metoda na vygenerovanie nahodneho pola s danou velkostou
	public static int[] generateRandomArray(int length) {
		int[] array = new int[length];
		for (int i = 0; i < array.length; i++) {
			array[i] = new Random().nextInt(2 * length);
		}
		return array;
	}

	// metoda na overenie, ci je pole usortene
	public static boolean isSorted(int[] array) {
		for (int i = 1; i < array.length; i++) {
			if (array[i] < array[i - 1]) {
				return false;
			}
		}
		return true;
	}

	// metoda na vypisovanie do konzoly
	public static void log(int processId, String msg) {
		String prefix;
		if (processId == 0) {
			prefix = "Master: ";
		} else {
			prefix = "Worker" + processId + ": ";
		}
		System.out.println(prefix + msg);
	}
}
