package sort_java;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Array;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


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

    private int getBucket(Item[] buckets, Item item, int numWork) {
      int lo = 0;
      int hi = numWork-2;

      if (item.compareTo(buckets[numWork-2]) > 0) {
        return (numWork-1);
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

    public sortBucketsCallable(Item[] buckets, Item[] array, List<List<List<Item>>> bucketLists, int numWork, int size, int id) {
      int numCalculate = size/numWork;
      int maxIdx = (id == (numWork-1)) ? size : (id+1)*numCalculate;

      for (int i = id * numCalculate; i < maxIdx; i += 1)
      {
        int bucket = getBucket(buckets, array[i], numWork);
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

    public consolidateBucketsCallable(List<List<List<Item>>> bucketLists, List<List<Item>> consolidatedBucketLists, int numWork, int id) {
      for (int i = 0; i < numWork; i += 1) {
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

    public threadSortCallable(List<List<Item>> consolidatedBucketLists, Item[] array, int numWork, int id) {
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
    int numWork = numProcessors * 30;
    int oversample_rate = 100;

    Random rand = new Random();
    Item[] samples = new Item[numWork*oversample_rate];

    for(int i = 0; i < numWork * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n];
    }
    
    Arrays.parallelSort(samples);

    Item[] buckets = new Item[numWork-1];

    for(int i = 1; i < numWork; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      //System.out.println("Bucket " + i + ": " + buckets[i-1].getHash());
    }

    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    List<List<List<Item>>> bucketLists = new ArrayList<List<List<Item>>>();

    for (int i = 0; i < numWork; i += 1) {
      List<List<Item>> bucketList = new ArrayList<List<Item>>();
      for (int j = 0; j < numWork; j += 1) {
        bucketList.add(new ArrayList<Item>());
      }
      bucketLists.add(bucketList);
    }

    for (int i = 0; i < numWork; i += 1)
    {
      Callable<Integer> callable = new sortBucketsCallable(buckets, array, bucketLists, numWork, size, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

    int sum = 0;
    while(sum < numWork){
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

    for (int i = 0; i < numWork; i += 1) {
      List<Item> bucketList = new ArrayList<Item>();
      consolidatedBucketLists.add(bucketList);
    }

    for (int i = 0; i < numWork; i += 1)
    {
      Callable<Integer> callable = new consolidateBucketsCallable(bucketLists, consolidatedBucketLists, numWork, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

    while(sum < numWork){
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

    for (int i = 0; i < numWork; i += 1)
    {
      Callable<Integer> callable = new threadSortCallable(consolidatedBucketLists, newArray, numWork, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

    executor.shutdown();

    while(sum < numWork){
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

  public static class mergeSortCallable
        implements Callable {
    private int done;

    public mergeSortCallable(Item[] array, Item[] newArray, int numPartitions, int size, int id) {
      int numCalculate = 4096;
      int startIdx = id*numCalculate;
      int maxIdx = (id == (numPartitions-1)) ? size : (id+1)*numCalculate;
      int tempSize = maxIdx - startIdx;

      Item[] tempArray = new Item[tempSize];
      System.arraycopy(array, startIdx, tempArray, 0, tempSize);

      Arrays.sort(tempArray);

      System.arraycopy(tempArray, 0, newArray, startIdx, tempSize);
      
      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static <T extends Comparable<? super T>> void MergeSortForkJoin(T[] a) {
    @SuppressWarnings("unchecked")
    T[] helper = (T[])Array.newInstance(a[0].getClass() , a.length);
    int numProcessors = Runtime.getRuntime().availableProcessors();
    ForkJoinPool forkJoinPool = new ForkJoinPool(numProcessors);
    forkJoinPool.invoke(new MergeSortTask<T>(a, helper, 0, a.length-1));
  }

  private static class MergeSortTask<T extends Comparable<? super T>> extends RecursiveAction{
    private static final long serialVersionUID = -749935388568367268L;
    private final T[] a;
    private final T[] helper;
    private final int lo;
    private final int hi;
    
    public MergeSortTask(T[] a, T[] helper, int lo, int hi){
      this.a = a;
      this.helper = helper;
      this.lo = lo;
      this.hi = hi;
    }

    @Override
    protected void compute() {
      if (lo>=hi) return;
      int mid = lo + (hi-lo)/2;
      MergeSortTask<T> left = new MergeSortTask<>(a, helper, lo, mid);
      MergeSortTask<T> right = new MergeSortTask<>(a, helper, mid+1, hi);
      invokeAll(left, right);
      merge(this.a, this.helper, this.lo, mid, this.hi);
    }
    private void merge(T[] a, T[] helper, int lo, int mid, int hi){
      for (int i=lo;i<=hi;i++){
        helper[i]=a[i];
      }
      int i=lo,j=mid+1;
      for(int k=lo;k<=hi;k++){
        if (i>mid){
          a[k]=helper[j++];
        }else if (j>hi){
          a[k]=helper[i++];
        }else if(isLess(helper[i], helper[j])){
          a[k]=helper[i++];
        }else{
          a[k]=helper[j++];
        }
      }
    }
    private boolean isLess(T a, T b) {
      return a.compareTo(b) < 0;
    }
  }

  public static Item[] mergesort(Item[] array)
  {

    int size = array.length;
    int numProcessors = Runtime.getRuntime().availableProcessors();

    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);

    int numPartitions = size/4096;
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();

    Item[] newArray = new Item[size];

    for(int i = 0; i < numPartitions; i += 1) {
      Callable<Integer> callable = new mergeSortCallable(array, newArray, numPartitions, size, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }

    int sum = 0;
    while(sum < numPartitions){
      sum = 0;
      for (Future<Integer> future : set) {
        try{
          sum += future.get();
        } catch (Exception e) {
          break;
        }
      }
    }
/*
    sum = 0;
    set.clear();

    for (int i = 0; i < numPartitions; i += 1)
    {
      Callable<Integer> callable = new mergeCallable(newArray, numPartitions, i);
      Future<Integer> future = executor.submit(callable);
      set.add(future);
    }*/

    executor.shutdown();

    /*while(sum < numPartitions){
      sum = 0;
      for (Future<Integer> future : set) {
        try{
          sum += future.get();
        } catch (Exception e) {
          break;
        }
      }
    }*/


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
      if(args[0].equals("p")) {
        version = 1;
      }
      if(args[0].equals("s")) {
        version = 2;
      }
      if(args[0].equals("m")) {
        version = 3;
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
      } else if (version == 2) {
        start_sort = System.nanoTime();
        sortedItems = Arrays.copyOf(items, size);
        Arrays.sort(sortedItems);
        end_sort = System.nanoTime();
      } else if (version == 3) {
        start_sort = System.nanoTime();
        sortedItems = Arrays.copyOf(items, size);
        MergeSortForkJoin(sortedItems);
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