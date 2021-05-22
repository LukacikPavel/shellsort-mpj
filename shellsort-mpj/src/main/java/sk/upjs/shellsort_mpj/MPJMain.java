package sk.upjs.shellsort_mpj;

import mpi.MPI;

public class MPJMain {

	public static void main( String[] args )
    {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        Runnable p = (me == 0) ? new MPJMaster() : new MPJWorker();
        p.run();
        MPI.Finalize();
    }

}
