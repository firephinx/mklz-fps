package main

import (
  "time"
  "fmt"
  "sort"
)

type count_struct_II struct {
  counts []int
  which_bucket []int
  in ElementSlice
}

type seq_sort_info struct {
  n_elems int
  init_duration time.Duration
  sort_duration time.Duration
  shuffle_duration time.Duration
  total_duration time.Duration
}

func count_II(in ElementSlice, bucket_walls ElementSlice, ch chan count_struct_II) {

  n := len(in)
  // The local per-bucket counts
  counts := make([]int, len(bucket_walls) + 1)
  
  // Copy
  local_walls :=make(ElementSlice, len(bucket_walls))
  copy(local_walls, bucket_walls)

  // Which buckets the elements belong to
  which_bucket := make([]int, n)

  for i := 0; i<n;i++ {
    low := 0
    high := len(local_walls)

    for high - low > 1 {
      mid := low + (high - low)/2
      if (&local_walls[mid]).Less(&in[i]) {
        low = mid
      } else {
        high = mid
      }
    }
    bidx := low

    if bidx == len(local_walls) + 1 {
      fmt.Println("Err")
    }
    which_bucket[i] = bidx

    counts[which_bucket[i]] += 1
  }

  // Report the local bucket counts to the master
  ch <- count_struct_II{counts, which_bucket, in}
}

func partition(in, out ElementSlice, which_bucket []int, bucket_offsets []int, counts []int, done chan bool) {
  n := len(in)

  for i:=0;i<n;i++ {
    bucket := which_bucket[i]
    out[bucket_offsets[bucket] + counts[bucket]] = in[i]
    counts[which_bucket[i]] += 1
  }

  done <- true
}

func parallel_sample_sort(in, out ElementSlice, ps ParamStruct) {

  // Read parameters defined via cmd-line
  threads := ps.threads
  // n_buckets := ps.n_buckets
  n_buckets := threads * 8
  oversample_stride := ps.oversample_stride

  // n_blocks := ps.n_countblocks
  n_blocks := threads * 4

  n := len(in)

// ========================================== Sample

  time_begin := time.Now()

  // Sample
  n_oversample := n_buckets * oversample_stride - 1
  oversamples := make(ElementSlice, n_oversample)
  
  for i:=0;i<n_oversample;i++ {
    random_index := hash64(uint64(i)) % uint64(n)
    oversamples[i] = in[random_index]
  }

  sort.Sort(oversamples)

  walls := make(ElementSlice, n_buckets-1)
  for i := range(walls) {
    walls[i] = oversamples[(i+1) * oversample_stride - 1]
  }

  sample_elapsed := time.Since(time_begin)  

// ========================================== Count

  time_begin_count := time.Now()

  // In parallel, count how many elements are in each bucket
  count_channel := make(chan count_struct_II)
  done_channel := make(chan bool)

  bucket_counts := make([]int, n_buckets)
  messages := make([]count_struct_II, n_blocks)

  for i:=0; i<n_blocks; i++ {
    // Matt: updated
    blk, _ := block(in, i, n_blocks)
    go count_II(blk, walls, count_channel)
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
    go partition(msg.in, out, msg.which_bucket, bucket_offsets, msg.counts, done_channel)
  }

  // Wait for partitioning to finish
  for i:=0;i < n_blocks;i++ {
    <- done_channel
  }

  partition_elapsed := time.Since(time_begin_partition)


  //____________________________________________________________________________
  //                                                     Sort within each bucket

  time_begin_sort := time.Now()

  // This is just for logging purposes
  seq_channel := make(chan seq_sort_info, n_buckets)

  // Sort within each partition
  for i:=0;i<n_buckets;i++ {
    // go sequential_sort(out[bucket_offsets[i]:bucket_offsets[i]+bucket_counts[i]], done_channel)
    // go sequential_sort_by_index(out[bucket_offsets[i]:bucket_offsets[i]+bucket_counts[i]], done_channel)

    if ps.sort_by_indices {
      go sequential_sort_by_index(out[bucket_offsets[i]:bucket_offsets[i]+bucket_counts[i]], seq_channel)
    } else {
      go sequential_sort(out[bucket_offsets[i]:bucket_offsets[i]+bucket_counts[i]], seq_channel)
    }

  }

  info_slice := make([]seq_sort_info, n_buckets)

  for i:=0;i<n_buckets;i++ {
    // <- done_channel
    // var info seq_sort_info
    info_slice[i] = <- seq_channel
  }

  // Sort info by # elems
  sort.Slice(info_slice, func(i, j int) bool { return info_slice[i].n_elems < info_slice[j].n_elems })

  sort_elapsed := time.Since(time_begin_sort)
  total_elapsed := time.Since(time_begin)

  if ps.print_seq_timing {
    for _,info := range(info_slice) {
          // fmt.Printf("Sorting %d elems by index:\n", info.n_elems)
      // fmt.Printf("Sort indices: %s\n", info.sort_duration)
      // fmt.Printf("Shuffle elems: %s\n", info.shuffle_duration)
      // fmt.Printf("Total: %s\n", info.total_duration)

      fmt.Printf("%d, %5f, %.5f, %.5f, %.5f\n", 
        info.n_elems, 
        info.init_duration.Seconds(), 
        info.sort_duration.Seconds(), 
        info.shuffle_duration.Seconds(), 
        info.total_duration.Seconds())
    }
  }

  if ps.verbose {
    fmt.Printf("Time taken to draw samples: %s\n", sample_elapsed)
    fmt.Printf("Time taken to count: %s\n", count_elapsed)
    fmt.Printf("Time taken to partition data: %s\n", partition_elapsed)
    fmt.Printf("Time taken to sort buckets: %s\n", sort_elapsed)
    fmt.Println()
    fmt.Printf("Total time for sample_sort: %s\n", total_elapsed)
  }
}
