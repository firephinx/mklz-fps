package sort_java;

import java.util.*;
import java.util.concurrent.*;

public class Item implements Comparable<Item> {
	private double hash;
	private double data;	
	public Item(int num) {
		this.hash = generateReal(num);
		this.data = num;
	}
	public int hash(int i)
    {
      long v = ((long) i) * 3935559000370003845L + 2691343689449507681L;
      v = v ^ (v >> 21);
      v = v ^ (v << 37);
      v = v ^ (v >> 4);
      v = v * 4768777513237032717L;
      v = v ^ (v << 20);
      v = v ^ (v >> 41);
      v = v ^ (v <<  5);
      return (int) (v & ((((long) 1) << 31) - 1));
    }

    // generates a pseudorandom double precision real from an integer
    public double generateReal(int i) {
        return (double) hash(i);
    }
	public double getHash() {
		return hash;
	}
	public double getData() {
		return data;
	}
	@Override
	public int compareTo(Item item) {
		return Double.valueOf(hash).compareTo(item.getHash());
	}

	public static class getItemCallable
        implements Callable {
    private int done;
    public getItemCallable(Item[] array, int numProcessors, int size, int id) {
      int numCalculate = size/numProcessors;
      int maxIdx = (id == (numProcessors-1)) ? size : (id+1)*numCalculate;

      for (int i = id * numCalculate; i < maxIdx; i += 1)
		  {
		    array[i] = new Item(i);
		  }
		  done = 1;
    }
    public Integer call() {
      return done;
    }
  }

	public static Item[] getItems(int size) {
		int numProcessors = Runtime.getRuntime().availableProcessors();
		System.out.println("numProcessors = " + numProcessors);
		ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
		Item[] items = new Item[size];
		Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    
		for (int i = 0; i < numProcessors; i += 1)
	  {
	    Callable<Integer> callable = new getItemCallable(items, numProcessors, size, i);
	    Future<Integer> future = executor.submit(callable);
	    set.add(future);
	  }
	  executor.shutdown();

	  int sum = 0;
	  while(sum < numProcessors){
	  	sum = 0;
	  	for (Future<Integer> future : set) {
	  		try{
	  			sum += future.get();
	  		} catch (Exception e) {
				  break;
				}
	    }
	  }
		return items;
	}
} 