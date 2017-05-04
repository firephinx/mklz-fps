package main

import (
  "time"
  "fmt"
  "sort"
)

func parallel_sample_sort(in, out ElementSlice, tweak ParamStruct) {
  for i:=0;i<len(in);i++ {
    out[i] = in[i]
  }
  sort.Sort(out)
}

// Partition strategy

// Sample.
// Choose n_blocks dividers
// Count how many elements fall within each type of divider
//   - This is done by binary searching on the dividers

// Write this to an array, and return.

type count_struct struct {
  counts []int
  which_bucket []int
  in []Pair
}

func count(in []Pair, bucket_walls []float64, ch chan count_struct) {


  n := len(in)
  // The local per-bucket counts
  counts := make([]int, len(bucket_walls) + 1)
  
  // Which buckets the elements belong to
  which_bucket := make([]int, n)

  for i := 0; i<n;i++ {
    // bidx := sort.SearchFloat64s(bucket_walls, in[i].x)
    bidx := i % (len(bucket_walls) + 1)
    if bidx == len(bucket_walls) + 1 {
      fmt.Println("Err")
    }
    which_bucket[i] = bidx

    counts[which_bucket[i]] += 1
  }

  // Report the local bucket counts to the master
  ch <- count_struct{counts, which_bucket, in}
}

func partition(in []Pair, out []Pair, which_bucket []int, bucket_offsets []int, counts []int, done chan bool) {
  n := len(in)

  for i:=0;i<n;i++ {
    bucket := which_bucket[i]
    out[bucket_offsets[bucket] + counts[bucket]] = in[i]
    counts[which_bucket[i]] += 1
  }

  done <- true
}


func old_sample_sort(input, output ElementSlice, ps ParamStruct) {
  n_threads := ps.threads
  
  //____________________________________________________________________________
  //                                        Sample the data for partition points

  time_begin := time.Now()
  
  n := len(input)
  n_buckets := n_threads
  oversample_stride := 4
  n_oversample := n_buckets * oversample_stride - 1

  // Oversample the sequence (sequential)
  oversamples := make([]float64, n_oversample)
  for i := 0; i < n_oversample; i++ {
    random_index := hash64(uint64(i)) % uint64(n)
    oversamples[i] = input[random_index].x
  }

  // Sort the oversamples
  sort.Sort(sort.Float64Slice(oversamples))

  // Get the actual bucket start points
  bucket_walls := make([]float64, n_buckets-1)
  for i:=0; i<n_buckets-1;i++ {
    bucket_walls[i] = oversamples[(i+1) * oversample_stride - 1]
  }

  sample_elapsed := time.Since(time_begin)  

  //____________________________________________________________________________
  //                     Count, for each block, how many elems go in each bucket

  time_begin_count := time.Now()

  // In parallel, count how many elements are in each bucket
  count_channel := make(chan count_struct)
  done_channel := make(chan bool)

  n_blocks := n_threads
  bucket_counts := make([]int, n_buckets)
  messages := make([]count_struct, n_blocks)
  // block_len := updiv(n, n_threads)

  fmt.Printf("nblocks: %v\n", n_blocks)
  for i:=0; i<n_blocks; i++ {
    // start := block_len * i
    // end := min(start + block_len, n)

    // Matt: updated
    blk, _ := block(input, i, n_blocks)

    go count(blk, bucket_walls, count_channel)
  }
  
  // Compute, for each block, the intra-bucket start positions
  for i:=0; i<n_blocks; i++ {
    msg := <-count_channel
    messages[i] = msg
    for j:=0; j<n_buckets; j++ {
      bucket_counts[j] += msg.counts[j]
      msg.counts[j] = bucket_counts[j] - msg.counts[j]
    }

  }

  // Turn bucket counts into global bucket start positions
  bucket_offsets := make([]int, n_buckets)
  for i:=1;i<n_buckets;i++ {
    bucket_offsets[i] = bucket_offsets[i-1] + bucket_counts[i-1]
  }

  count_elapsed := time.Since(time_begin_count)

  //____________________________________________________________________________
  //       Copy elements from the input into their correct buckets in the output

  time_begin_partition := time.Now()

  for i:=0;i<n_blocks;i++ {
    msg := messages[i]
    go partition(msg.in, output, msg.which_bucket, bucket_offsets, msg.counts, done_channel)
  }

  // Wait for partitioning to finish
  for i:=0;i < n_blocks;i++ {
    <- done_channel
  }

  partition_elapsed := time.Since(time_begin_partition)

  // Confirm that the partitioning has taken place correctly
  // verify_partition(bucket_walls, bucket_offsets, bucket_counts, output)

  //____________________________________________________________________________
  //                                                     Sort within each bucket

  time_begin_sort := time.Now()

  // Sort within each partition
  for i:=0;i<n_buckets;i++ {
    go sequential_sort(output[bucket_offsets[i]:bucket_offsets[i]+bucket_counts[i]], done_channel)
  }

  for i:=0;i<n_buckets;i++ {
    <- done_channel
  }

  sort_elapsed := time.Since(time_begin_sort)

  total_elapsed := time.Since(time_begin)

  
  fmt.Printf("Time taken to draw samples: %s\n", sample_elapsed)
  fmt.Printf("Time taken to get bucket counts: %s\n", count_elapsed)
  fmt.Printf("Time taken to partition data: %s\n", partition_elapsed)
  fmt.Printf("Time taken to sort buckets: %s\n", sort_elapsed)
  fmt.Println()
  fmt.Printf("Total time for sample_sort: %s\n", total_elapsed)
}