import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

class Result {

	/*
	 * Complete the 'maximumSum' function below.
	 *
	 * The function is expected to return a LONG_INTEGER. The function accepts
	 * INTEGER_ARRAY arr as parameter.
	 */

	public static long maximumSum(List<Integer> arr) {
		System.out.println("Result.maximumSum() list is: " + arr);
		long longestSum = arr.get(arr.size() - 1);
		for (int start = arr.size() - 1; start > -1; start--) {
			long newLongestSum = getLongestSumStartingAtFirst(arr.subList(start, arr.size()));
			if (newLongestSum > longestSum) {
				longestSum = newLongestSum;
			}
		}
		return longestSum;
	}

	private static long getLongestSumStartingAtFirst(List<Integer> subList) {
		long longestSumFromFirst = Integer.MIN_VALUE;
		long sumFromFirst = 0;
		for (int next : subList) {
			sumFromFirst += next;
			if (sumFromFirst > longestSumFromFirst) {
				longestSumFromFirst = sumFromFirst;
			}
		}
		return longestSumFromFirst;
	}

	@Deprecated
	public static long maximumSumOld(List<Integer> arr) {
		// We cannot optimize this with tail recursion because the tail's
		// longest subsequence may not be at the edge of the list

		long sum = arr.get(0);
		System.out.println("Sridhar: " + arr);
		for (int start = 0; start < arr.size() + 1; start++) {
			for (int i = start + 1; i < arr.size() + 1; i++) {
				long sumIter = sum(arr.subList(start, i));
				System.out.println("Sridhar: " + arr.subList(start, i));
				if (sum < sumIter) {
					sum = sumIter;
				}
			}
			System.out.println("---------");
		}

		return sum;
	}

	@Deprecated
	private static long sum(List<Integer> subList) {
		long sum = 0;
		for (int elem : subList) {
			sum += elem;
		}
		System.out.println("Sridhar: " + subList + " =  " + sum);
		return sum;
	}

}

public class Solution {
	public static void main(String[] args) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

		int arrCount = Integer.parseInt(bufferedReader.readLine().trim());

		List<Integer> arr = new ArrayList<>();

		for (int i = 0; i < arrCount; i++) {
			int arrItem = Integer.parseInt(bufferedReader.readLine().trim());
			arr.add(arrItem);
		}

		long result = Result.maximumSum(arr);

		bufferedWriter.write(String.valueOf(result));
		bufferedWriter.newLine();

		bufferedReader.close();
		bufferedWriter.close();
	}
}
