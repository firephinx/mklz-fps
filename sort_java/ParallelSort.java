package sort_java;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.*;
import java.util.concurrent.*;


public class ParallelSort
{
   /**
    * Sorts the array using a parallel sample sort algorithm
    * @param array The array to be sorted
    * @return The sorted array
    */

  public static class sortBucketsCallable
        implements Callable {
    private int done;

    private int getBucket(Item[] buckets, Item item, int numProcessors) {
      int lo = 0;
      int hi = numProcessors-2;

      if (item.compareTo(buckets[numProcessors-2]) > 0) {
        return (numProcessors-1);
      } else {
        while (lo <= hi) {
            // Key is in a[lo..hi] or not present.
            int mid = lo + (hi - lo) / 2;
            if      (item.compareTo(buckets[mid]) < 0) hi = mid - 1;
            else if (item.compareTo(buckets[mid]) > 0) lo = mid + 1;
            else return mid;
        }
        return lo;
      }
    }

    public sortBucketsCallable(Item[] buckets, Item[] array, List<List<List<Item>>> bucketLists, int numProcessors, int size, int id) {
      int numCalculate = size/numProcessors;
      int maxIdx = (id == (numProcessors-1)) ? size : (id+1)*numCalculate;

      for (int i = id * numCalculate; i < maxIdx; i += 1)
      {
        int bucket = getBucket(buckets, array[i], numProcessors);
        bucketLists.get(id).get(bucket).add(array[i]);
      }
      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static class consolidateBucketsCallable
        implements Callable {
    private int done;

    public consolidateBucketsCallable(List<List<List<Item>>> bucketLists, List<List<Item>> consolidatedBucketLists, int numProcessors, int id) {
      for (int i = 0; i < numProcessors; i += 1) {
        consolidatedBucketLists.get(id).addAll(bucketLists.get(i).get(id));
      }
      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static class threadSortCallable
        implements Callable {
    private int done;

    public threadSortCallable(List<List<Item>> consolidatedBucketLists, Item[] array, int numProcessors, int id) {
      int size = consolidatedBucketLists.get(id).size();
      int start = 0;

      for (int i = 0; i < id; i += 1) {
        start += consolidatedBucketLists.get(i).size();
      }

      Item[] newArray = consolidatedBucketLists.get(id).toArray(new Item[size]);

      Arrays.sort(newArray);

      System.arraycopy(newArray, 0, array, start, size);

      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static Item[] sort(Item[] array)
  {

    int size = array.length;
    int numProcessors = Runtime.getRuntime().availableProcessors();
    int oversample_rate = 1000;

    Random rand = new Random();
    Item[] samples = new Item[numProcessors*oversample_rate];

    for(int i = 0; i < numProcessors * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n];
    }
    
    Arrays.parallelSort(samples);

    Item[] buckets = new Item[numProcessors-1];

    for(int i = 1; i < numProcessors; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      //System.out.println("Bucket " + i + ": " + buckets[i-1].getHash());
    }

    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    List<List<List<Item>>> bucketLists = new ArrayList<List<List<Item>>>();

    for (int i = 0; i < numProcessors; i += 1) {
      List<List<Item>> bucketList = new ArrayList<List<Item>>();
      for (int j = 0; j < numProcessors; j += 1) {
        bucketList.add(new ArrayList<Item>());
      }
      bucketLists.add(bucketList);
    }

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new sortBucketsCallable(buckets, array, bucketLists, numProcessors, size, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

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

    sum = 0;
    set.clear();

    List<List<Item>> consolidatedBucketLists = new ArrayList<List<Item>>();

    for (int i = 0; i < numProcessors; i += 1) {
      List<Item> bucketList = new ArrayList<Item>();
      consolidatedBucketLists.add(bucketList);
    }

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new consolidateBucketsCallable(bucketLists, consolidatedBucketLists, numProcessors, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

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

    sum = 0;
    set.clear();

    Item[] newArray = new Item[size];  

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new threadSortCallable(consolidatedBucketLists, newArray, numProcessors, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

    executor.shutdown();

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

    return newArray;
    
  }

  public static int hash(int i)
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
 public static double generateReal(int i) {
    return (double) hash(i);
 }

  public static void main(String args[])
  {
    int version = 0;
    if (args.length > 0) {
      if(args[0].equals("o")) {
        version = 1;
      }
    }

    //Create the variables for timing
    double start_generation = 0.0;
    double end_generation = 0.0;
    double duration_generation = 0.0;
    double start_sort = 0.0;
    double end_sort = 0.0;
    double duration_sort = 0.0;
    double start_check = 0.0;
    double end_check = 0.0;
    double duration_check = 0.0;

    //Number of elements to sort
    int size = 100000000;
    int numRuns = 5;

    if(version == 0 || version == 1) {
      start_generation = System.nanoTime();
      Item[] items = Item.getItems(size);
      end_generation = System.nanoTime();

      duration_generation = (end_generation - start_generation) / 1E9;
      System.out.println("Generation Duration: " + duration_generation);

      for(int run = 0; run <= numRuns; run += 1)
      {
        Item[] sortedItems;
        if (version == 1) {
          start_sort = System.nanoTime();
          sortedItems = Arrays.copyOf(items, size);
          Arrays.parallelSort(sortedItems);
          end_sort = System.nanoTime();
        } else {
          start_sort = System.nanoTime();
          sortedItems = sort(items);
          end_sort = System.nanoTime();
        } 

        start_check = System.nanoTime();
        for(int i = 1; i < size; i++){
          if(sortedItems[i].compareTo(sortedItems[i-1]) < 0) {
            System.out.println("Failed Correctness" + sortedItems[i-1].getHash() + " is before " + sortedItems[i].getHash());
          }
        }
        end_check = System.nanoTime();

        //Output performance results
        if(run > 0) {
          duration_sort = (end_sort - start_sort) / 1E9;
          duration_check = (end_check - start_check) / 1E9;
          System.out.println("Sort Duration: " + duration_sort);
          System.out.println("Check Duration: " + duration_check);
        }
      }
    }
  }
}
