package headless;

import java.util.Arrays;

public class TestHash {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int[] w = {-1,-1};
		int[] l = {-1,0};
		
		System.out.printf("h(w): %d\n",Arrays.hashCode(w));
		System.out.printf("h(l): %d\n",Arrays.hashCode(l));
		
		Arrays.sort(w);
		Arrays.sort(l);
		
		System.out.printf("h(s_w): %d\n",Arrays.hashCode(w));
		System.out.printf("h(s_l): %d\n",Arrays.hashCode(l));
	}

}
