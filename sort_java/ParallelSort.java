package sort_java;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
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

  public static <T extends Comparable<? super T>> void MergeSortForkJoinGranularity(T[] a) {
    @SuppressWarnings("unchecked")
    T[] helper = (T[])Array.newInstance(a[0].getClass() , a.length);
    int numProcessors = Runtime.getRuntime().availableProcessors();
    ForkJoinPool forkJoinPool = new ForkJoinPool(numProcessors);
    forkJoinPool.invoke(new MergeSortTaskGranularity<T>(a, helper, 0, a.length-1));
  }

  private static class MergeSortTaskGranularity<T extends Comparable<? super T>> extends RecursiveAction{
    private static final long serialVersionUID = -749935388568367268L;
    private static final int granularity = 4096;
    private final T[] a;
    private final T[] helper;
    private final int lo;
    private final int hi;
    
    public MergeSortTaskGranularity(T[] a, T[] helper, int lo, int hi){
      this.a = a;
      this.helper = helper;
      this.lo = lo;
      this.hi = hi;
    }

    @Override
    protected void compute() {
      if (lo>=hi) return;
      int mid = lo + (hi-lo)/2;
      if(hi - lo < granularity){
        Arrays.sort(a, lo, hi+1);
        return;
      }
      MergeSortTaskGranularity<T> left = new MergeSortTaskGranularity<>(a, helper, lo, mid);
      MergeSortTaskGranularity<T> right = new MergeSortTaskGranularity<>(a, helper, mid+1, hi);
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

    public sortBucketsCallable(Item[] buckets, Item[] array, int[] bucketLists, List<int[]> bucketCounts, int numProcessors, int size, int id) {
      int numCalculate = size/numProcessors;
      int maxIdx = (id == (numProcessors-1)) ? size : (id+1)*numCalculate;
      int[] bucketCountArray = bucketCounts.get(id);

      for (int i = id * numCalculate; i < maxIdx; i += 1)
      {
        int bucket = getBucket(buckets, array[i], numProcessors);
        bucketLists[i] = bucket;
        bucketCountArray[bucket] += 1;
      }
      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static class transferDataCallable
        implements Callable {
    private int done;

    public transferDataCallable(int[] bucketLists, List<int[]> bucketStarts, int[] bucketSizeOffsets, Item[] array, Item[] newArray, int numProcessors, int size, int id) {
      int numCalculate = size/numProcessors;
      int maxIdx = (id == (numProcessors-1)) ? size : (id+1)*numCalculate;
      int[] bucketStart = bucketStarts.get(id);
      int[] bucketCount = new int[numProcessors];

      for(int i = 0; i < numProcessors; i += 1) {
        bucketStart[i] += bucketSizeOffsets[i];
      }

      for (int i = id * numCalculate; i < maxIdx; i += 1)
      {
        int bucket = bucketLists[i];
        int newIdx = bucketStart[bucket] + bucketCount[bucket];
        bucketCount[bucket] += 1;
        newArray[newIdx] = array[i];
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

    public threadSortCallable(Item[] newArray, int[] bucketSizeOffsets, int id) {
      //int size = bucketSizeOffsets[id+1] - bucketSizeOffsets[id];
      //System.out.println("Bucket " + id + " size: " + size);

      Arrays.parallelSort(newArray, bucketSizeOffsets[id], bucketSizeOffsets[id+1]);

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
    int oversample_rate = 250;

    Random rand = new Random();
    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    int sum = 0;

    double start_sampling = System.nanoTime();
    Item[] samples = new Item[numProcessors*oversample_rate];

    for(int i = 0; i < numProcessors * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n];
    }
    double end_sampling = System.nanoTime();
    double duration_sampling = (end_sampling - start_sampling) / 1E9;
    System.out.println("Sampling Duration: " + duration_sampling);
    
    double start_sort_samples = System.nanoTime();
    Arrays.parallelSort(samples);
    double end_sort_samples = System.nanoTime();
    double duration_sort_samples = (end_sort_samples - start_sort_samples) / 1E9;
    System.out.println("Sort Samples Duration: " + duration_sort_samples);

    double start_making_buckets = System.nanoTime();
    Item[] buckets = new Item[numProcessors-1];

    for(int i = 1; i < numProcessors; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      //System.out.println("Bucket " + i + ": " + buckets[i-1].getHash());
    }
    double end_making_buckets = System.nanoTime();
    double duration_making_buckets = (end_making_buckets - start_making_buckets) / 1E9;
    System.out.println("Making Buckets Duration: " + duration_making_buckets);

    double start_bucket_counts = System.nanoTime();
    int[] bucketLists = new int[size];
    List<int[]> bucketCounts = new ArrayList<int[]>();

    for (int i = 0; i < numProcessors; i += 1)
    {
      int[] bucketCount = new int[numProcessors];
      bucketCounts.add(bucketCount);
    }

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new sortBucketsCallable(buckets, array, bucketLists, bucketCounts, numProcessors, size, i);
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

    double end_bucket_counts = System.nanoTime();
    double duration_bucket_counts = (end_bucket_counts - start_bucket_counts) / 1E9;
    System.out.println("Bucket Counts Duration: " + duration_bucket_counts);

    double start_prefix_sums = System.nanoTime();
    List<int[]> bucketStarts = new ArrayList<int[]>();
    int[] startingBucket = new int[numProcessors];
    bucketStarts.add(startingBucket);

    for (int i = 0; i < numProcessors; i += 1) {
      int[] prevBucketStarts = bucketStarts.get(i);
      int[] bucketStart = new int[numProcessors];
      int[] bucketArray = bucketCounts.get(i);
      for (int j = 0; j < numProcessors; j += 1) {
        bucketStart[j] += prevBucketStarts[j] + bucketArray[j];
      }
      bucketStarts.add(bucketStart);
    }

    int[] bucketSizes = bucketStarts.get(numProcessors);
    int[] bucketSizeOffsets = new int[numProcessors+1];

    for (int i = 0; i < numProcessors; i += 1) {
      bucketSizeOffsets[i+1] = bucketSizeOffsets[i] + bucketSizes[i];
    }

    double end_prefix_sums = System.nanoTime();
    double duration_prefix_sums = (end_prefix_sums - start_prefix_sums) / 1E9;
    System.out.println("Prefix Sum Duration: " + duration_prefix_sums);

    sum = 0;
    set.clear();

    double start_transfer_data = System.nanoTime();
    Item[] newArray = new Item[size];  

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new transferDataCallable(bucketLists, bucketStarts, bucketSizeOffsets, array, newArray, numProcessors, size, i);
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

    double end_transfer_data = System.nanoTime();
    double duration_transfer_data = (end_transfer_data - start_transfer_data) / 1E9;
    System.out.println("Transfer Data Duration: " + duration_transfer_data);


    sum = 0;
    set.clear();

    double start_sort_buckets = System.nanoTime(); 

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new threadSortCallable(newArray, bucketSizeOffsets, i);
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

    executor.shutdown();

    double end_sort_buckets = System.nanoTime();
    double duration_sort_buckets = (end_sort_buckets - start_sort_buckets) / 1E9;
    System.out.println("Sorting Bucket Lists Duration: " + duration_sort_buckets);

    return newArray;
    
  }

  public static class sortBucketsLockingCallable
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

    public sortBucketsLockingCallable(Item[] buckets, Item[] array, List<List<Item>> bucketLists, List<ReentrantLock> locks, int numProcessors, int numWork, int size, int id) {
      int numCalculate = size/numProcessors;
      int maxIdx = (id == (numProcessors-1)) ? size : (id+1)*numCalculate;

      for (int i = id * numCalculate; i < maxIdx; i += 1)
      {
        int bucket = getBucket(buckets, array[i], numWork);
        locks.get(bucket).lock();
        try {
          bucketLists.get(bucket).add(array[i]);
        } finally {
          locks.get(bucket).unlock();
        }
      }
      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static class threadSortLockingCallable
        implements Callable {
    private int done;

    public threadSortLockingCallable(List<List<Item>> bucketLists, Item[] array, int numWork, int id) {
      int size = bucketLists.get(id).size();
      //System.out.println("Bucket " + id + " size: " + size);
      int start = 0;

      for (int i = 0; i < id; i += 1) {
        start += bucketLists.get(i).size();
      }

      Item[] newArray = bucketLists.get(id).toArray(new Item[size]);
      Arrays.parallelSort(newArray);
      System.arraycopy(newArray, 0, array, start, size);

      done = 1;
    }
    public Integer call() {
      return done;
    }
  }

  public static Item[] sortLocking(Item[] array)
  {

    int size = array.length;
    int numProcessors = Runtime.getRuntime().availableProcessors();
    int numWork = numProcessors;
    int oversample_rate = 500;

    Random rand = new Random();
    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
    Set<Future<Integer>> set = new HashSet<Future<Integer>>();
    int sum = 0;

    double start_sampling = System.nanoTime();
    Item[] samples = new Item[numWork*oversample_rate];

    for(int i = 0; i < numWork * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n];
    }
    double end_sampling = System.nanoTime();
    double duration_sampling = (end_sampling - start_sampling) / 1E9;
    System.out.println("Sampling Duration: " + duration_sampling);
    
    double start_sort_samples = System.nanoTime();
    Arrays.parallelSort(samples);
    double end_sort_samples = System.nanoTime();
    double duration_sort_samples = (end_sort_samples - start_sort_samples) / 1E9;
    System.out.println("Sort Samples Duration: " + duration_sort_samples);

    double start_making_buckets = System.nanoTime();
    Item[] buckets = new Item[numWork-1];

    for(int i = 1; i < numWork; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      //System.out.println("Bucket " + i + ": " + buckets[i-1].getHash());
    }
    double end_making_buckets = System.nanoTime();
    double duration_making_buckets = (end_making_buckets - start_making_buckets) / 1E9;
    System.out.println("Making Buckets Duration: " + duration_making_buckets);

    double start_bucket_lists = System.nanoTime();
    List<List<Item>> bucketLists = new ArrayList<List<Item>>();
    List<ReentrantLock> locks = new ArrayList<ReentrantLock>();

    for (int i = 0; i < numWork; i += 1) {
      List<Item> bucketList = new ArrayList<Item>();
      ReentrantLock lock = new ReentrantLock();
      bucketLists.add(bucketList);
      locks.add(lock);
    }

    for (int i = 0; i < numProcessors; i += 1)
    {
      Callable<Integer> callable = new sortBucketsLockingCallable(buckets, array, bucketLists, locks, numProcessors, numWork, size, i);
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

    double end_bucket_lists = System.nanoTime();
    double duration_bucket_lists = (end_bucket_lists - start_bucket_lists) / 1E9;
    System.out.println("Bucket Lists Duration: " + duration_bucket_lists);

    sum = 0;
    set.clear();

    double start_sort_buckets = System.nanoTime();
    Item[] newArray = new Item[size];  

    for (int i = 0; i < numWork; i += 1)
    {
      Callable<Integer> callable = new threadSortLockingCallable(bucketLists, newArray, numWork, i);
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

    executor.shutdown();

    double end_sort_buckets = System.nanoTime();
    double duration_sort_buckets = (end_sort_buckets - start_sort_buckets) / 1E9;
    System.out.println("Sorting Bucket Lists Duration: " + duration_sort_buckets);

    return newArray;
    
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
      if(args[0].equals("g")) {
        version = 4;
      }
      if(args[0].equals("l")) {
        version = 5;
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
      } else if (version == 4) {
        start_sort = System.nanoTime();
        sortedItems = Arrays.copyOf(items, size);
        MergeSortForkJoinGranularity(sortedItems);
        end_sort = System.nanoTime();
      } else if (version == 5) {
        start_sort = System.nanoTime();
        sortedItems = sortLocking(items);
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
