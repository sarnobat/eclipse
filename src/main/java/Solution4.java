import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;



class Result {

    /*
     * Complete the 'weightCapacity' function below.
     *
     * The function is expected to return an INTEGER.
     * The function accepts following parameters:
     *  1. INTEGER_ARRAY weights
     *  2. INTEGER maxCapacity
     */

    public static int weightCapacity(List<Integer> weights, int maxCapacity) {
    	Collections.sort(weights);
    	Collections.reverse(weights);
    	System.out.println("capacity: " + maxCapacity);
    	System.out.println("weights: " + weights);
    	int runningTotal = 0;
    	
    	return helper(weights, maxCapacity, 0);
    	
    }
    
    private static int helper(List<Integer> weights, int maxCapacity, int runningTotal) {
    	
    	
    	if (weights.get(0) + runningTotal < maxCapacity) {
    		return helper(weights.subList(1, weights.size()), maxCapacity - weights.get(0), weights.get(0) + runningTotal);
    	} else if (weights.get(weights.size()-1) + runningTotal < maxCapacity) {
    		return helper(weights.subList(0, weights.size()-1), maxCapacity- weights.get(weights.size()-1), weights.get(weights.size()-1) - runningTotal);
    	}
    	return runningTotal;
    }
    
//    	int i = 0;
//    	int j = weights.size();
//    	while (i <= j) {
//    		
//    	}
//    	for (int next : weights) {
//    		int newTotal = next + runningTotal;
//    		if (newTotal <= maxCapacity)  {
//    			runningTotal = newTotal;
//    		}
//    	}
//    	
//    	int runningTotal2 = 0;
//    	for (int i = weights.size(); i > 0; i++) {
//    		
//    	}
//    	
//    	return Integer.max(runningTotal, runningTotal2);
//    }

}

public class Solution4 {
    public static void main(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        int weightsCount = Integer.parseInt(bufferedReader.readLine().trim());

        List<Integer> weights = new ArrayList<>();

        for (int i = 0; i < weightsCount; i++) {
            int weightsItem = Integer.parseInt(bufferedReader.readLine().trim());
            weights.add(weightsItem);
        }

        int maxCapacity = Integer.parseInt(bufferedReader.readLine().trim());

        int result = Result.weightCapacity(weights, maxCapacity);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedReader.close();
        bufferedWriter.close();
    }
}
