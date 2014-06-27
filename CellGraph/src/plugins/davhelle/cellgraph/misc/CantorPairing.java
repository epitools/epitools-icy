package plugins.davhelle.cellgraph.misc;

/**
 * Uniquely identify a couple of cells marked with integer numbers
 * adapted from:
 * 
 * http://de.wikipedia.org/wiki/Cantorsche_Paarungsfunktion
 * 
 * @author Davide Heller
 *
 */
public class CantorPairing {

	public static long compute(int a, int b) {
		long x = (long)a;
		long y = (long)b;
		return (x+y)*(x+y+1)/2 + y;
	}
	
	public static int[] reverse(long z){
		int a = (int) computeX(z);
		int b = (int) computeY(z);
		int[] input_pair = {a,b};
		return input_pair;
	}
	
	private static long computeX(long z) {
		long j  = (long) Math.floor(Math.sqrt(0.25 + 2*z) - 0.5);
		return j - (z - j*(j+1)/2);
	}
	
	private static long computeY(long z) {
		long j  = (long) Math.floor(Math.sqrt(0.25 + 2*z) - 0.5);
		return z - j*(j+1)/2;
	}


}
