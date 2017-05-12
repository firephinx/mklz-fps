# Fast Parallel Sort
By: Matthew Lee (mkl1) and Kevin Zhang (klz1)

## Summary

We submitted a fast parallel sample sorting algorithm in Go into [15-210's Sorting Competition](http://www.cs.cmu.edu/~15210/sort.html) by Professor Guy Blelloch. We optimized our implementation for performance on a 72-core machine called Aware and also tested on a variety of other machines such as the 20 core unix machines and the gates machines. After the competition, we analyzed and compared our performance with the provided code from the competition as well as parallel sorting implementations in other languages.

## Background

Sorting is a classical problem in computer science. The challenge we had was to correctly optimize the performance of different sorting algorithms on large parallel and distributed machines. The key input and output data structure was an array that needed to be sorted with key value pairs that were both doubles. Sorting is fundamentally not a computationally expensive problem and more a memory expensive problem. The benefit of parallelization in sorting comes from the ability to sort partitions of the array independently at the same time.

The dependencies in the program depend on the sorting algorithm. For example, with merge sort, the dependencies are like a tree where each branch needs to wait for its adjacent branch to complete before it can merge together and move up to the next merge. The parallelism with merge sort is that we can use fork join parallelism to compute different sections of the tree independently with a work stealing queue.

With sample sort, there are a few initial dependencies where the program needs to first sample elements from the array, sort them, and then determine the splitters between buckets. Then the processors can fill up each bucket in parallel by working on a segment of the array. Finally each bucket can be sorted in parallel.

## Approach




## Results



## References

Axtmann, M., & Sanders, P. (2017). Robust Massively Parallel Sorting. In 2017 Proceedings of the Ninteenth Workshop on Algorithm Engineering and Experiments (ALENEX) (pp. 83-97). Society for Industrial and Applied Mathematics.

## List of Work by Each Student

Matthew wrote all of the Go code while Kevin worked on the Java and CUDA sorting programs. In addition, Matthew made the presentation while Kevin worked on the final report.
