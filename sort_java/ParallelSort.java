package sort_java;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;


public class ParallelSort
{
   /**
    * Sorts the array using a parallel sample sort algorithm
    * @param array The array to be sorted
    * @return The sorted array
    */
  /*public static void sort(double[][] array)
  {

    int size = array.length/2;
    int numProcessors = Runtime.getRuntime().availableProcessors();
    int oversample_rate = 100;

    Random rand = new Random();
    double[] samples = new double[numProcessors*oversample_rate];

    for(int i = 0; i < numProcessors * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n][0];
    }
    
    Arrays.sort(samples);

    double[] buckets = new double[numProcessors-1];

    for(int i = 1; i < numProcessors; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      System.out.println("Bucket: " + buckets[i-1]);
    }

    Arrays.parallelSort(array, (double[] s1, double[] s2) -> Double.compare(s1[0],s2[0]));
    
  }*/
  public static void sort(Item[] array)
  {

    int size = array.length;
    int numProcessors = Runtime.getRuntime().availableProcessors();
    int oversample_rate = 100;

    Random rand = new Random();
    Item[] samples = new Item[numProcessors*oversample_rate];

    for(int i = 0; i < numProcessors * oversample_rate; i += 1)
    {
      int n = rand.nextInt(size);
      samples[i] = array[n];
    }
    
    Arrays.sort(samples);

    Item[] buckets = new Item[numProcessors-1];

    for(int i = 1; i < numProcessors; i += 1)
    {
      buckets[i-1] = samples[oversample_rate*i];
      System.out.println("Bucket " + i + ": " + buckets[i-1].getHash());
    }

    Arrays.parallelSort(array);
    
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
      else if (args[0].equals("a")) {
        version = 2;
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

    for(int run = 0; run <= numRuns; run += 1)
    {
      if(version == 0){
        start_generation = System.nanoTime();
        Item[] items = Item.getItems(size);
        end_generation = System.nanoTime();

        start_sort = System.nanoTime();
        sort(items);
        end_sort = System.nanoTime();

        start_check = System.nanoTime();
        for(int i = 1; i < size; i++){
          if(items[i].getHash() < items[i-1].getHash()) {
            System.out.println("Failed Correctness" + items[i-1].getHash() + " is before " + items[i].getHash());
          }
        }
        end_check = System.nanoTime();
      }
      else if(version == 1){
        start_generation = System.nanoTime();
        Item[] items = Item.getItems(size);
        end_generation = System.nanoTime();

        start_sort = System.nanoTime();
        Arrays.parallelSort(items);
        end_sort = System.nanoTime();

        start_check = System.nanoTime();
        for(int i = 1; i < size; i++){
          if(items[i].getHash() < items[i-1].getHash()) {
            System.out.println("Failed Correctness" + items[i-1].getHash() + " is before " + items[i].getHash());
          }
        }
        end_check = System.nanoTime();
      }
      else if(version == 2){
        start_generation = System.nanoTime();
        // Storing items into a 2D array
        double[][] items = new double[size*2][2];

        for(int i = 0; i < size; i+= 1) {
          items[i][0] = generateReal(i);
          items[i][1] = (double) i;
        }
        end_generation = System.nanoTime();

        start_sort = System.nanoTime();
        Arrays.parallelSort(items, (double[] s1, double[] s2) -> Double.compare(s1[0],s2[0]));
        end_sort = System.nanoTime();

        start_check = System.nanoTime();
        for(int i = 1; i < size; i++){
          if(items[i][0] < items[i-1][0]) {
            System.out.println("Failed Correctness" + items[i-1][0] + " is before " + items[i][0]);
          }
        }
        end_check = System.nanoTime();
      } 

      //Output performance results
      duration_generation = (end_generation - start_generation) / 1E9;
      duration_sort = (end_sort - start_sort) / 1E9;
      duration_check = (end_check - start_check) / 1E9;
      System.out.println("Generation Duration: " + duration_generation);
      System.out.println("Sort Duration: " + duration_sort);
      System.out.println("Check Duration: " + duration_check);
    }
  }
}
