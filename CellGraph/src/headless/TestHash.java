package headless;

import java.util.Arrays;

public class TestHash {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int[] w = {6,99,0};
		int[] l = {99,6};
		
		System.out.printf("h(w): %d\n",Arrays.hashCode(w));
		System.out.printf("h(l): %d\n",Arrays.hashCode(l));
		
		Arrays.sort(w);
		Arrays.sort(l);
		
		System.out.printf("h(s_w): %d\n",Arrays.hashCode(w));
		System.out.printf("h(s_l): %d\n",Arrays.hashCode(l));
	}

}
