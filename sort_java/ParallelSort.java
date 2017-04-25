package sort_java;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

public class ParallelSort
{
 /**
  * Sorts the array using a merge sort algorithm
  * @param array The array to be sorted
  * @return The sorted array
  */
 public static void sort(double[] array)
 {
      if(array.length > 1)
      {
           int centre;
           double[] left;
           double[] right;
           int arrayPointer = 0;
           int leftPointer = 0;
           int rightPointer = 0;

           centre = (int)Math.floor((array.length) / 2.0);

           left = new double[centre];
           right = new double[array.length - centre];

           System.arraycopy(array,0,left,0,left.length);
           System.arraycopy(array,centre,right,0,right.length);

           sort(left);
           sort(right);

           while((leftPointer < left.length) && (rightPointer < right.length))
           {
                if(left[leftPointer] <= right[rightPointer])
                {
                     array[arrayPointer] = left[leftPointer];
                     leftPointer += 1;
                }
                else
                {
                     array[arrayPointer] = right[rightPointer];
                     rightPointer += 1;
                }
                arrayPointer += 1;
           }
           if(leftPointer < left.length)
           {
                System.arraycopy(left,leftPointer,array,arrayPointer,array.length - arrayPointer);
           }
           else if(rightPointer < right.length)
           {
                System.arraycopy(right,rightPointer,array,arrayPointer,array.length - arrayPointer);
           }
      }
 }

 public static void main(String args[])
 {
      //Number of elements to sort
      int size = 100000000;

      //Create the variables for timing
      double start;
      double end;
      double duration;

      Item[] items = Item.getItems(size);
      //java.util.Collections.shuffle(items);

      //Run performance test
      start = System.nanoTime();
      Arrays.parallelSort(items);
      //sort(items);
      end = System.nanoTime();

      //Output performance results
      duration = (end - start) / 1E9;
      System.out.println("Duration: " + duration);
 }
}