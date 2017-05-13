package main

import (
  "os"
  "fmt"
  "strconv"
  "time"
  // "runtime"
  "sort"
  "math"
  // "math/rand"
)

type Pair struct {
  x float64
  y float64
}

func hash64(u uint64) uint64 {
  v := u * 3935559000370003845 + 2691343689449507681
  v ^= v >> 21
  v ^= v << 37
  v ^= v >>  4
  v *= 4768777513237032717
  v ^= v << 20
  v ^= v >> 41
  v ^= v <<  5
  return v
}

func min(x,y int) int {
  if x < y {
    return x
  }
  return y
}

func updiv(x, y int) int {
  return (x + y - 1) / y
}

func string_to_int(s string) int {
  x, err := strconv.Atoi(s)
  if err != nil {
      fmt.Println(err)
      os.Exit(1)
  }
  return x
}

func read_cmdline_input(args []string) (int, int, int) {
  const expected_args int = 3
  if num_args := len(args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <n_threads>\n", args[0])
    os.Exit(1);
  }
  return string_to_int(args[1]), string_to_int(args[2]), string_to_int(args[3])
}

func fill(a []Pair, base int, c chan int) {
  for i := 0; i < len(a); i++ {
    a[i].x = float64(hash64(uint64(i + base)))
    // a[i].x = rand.Float64()*10
    a[i].y = float64(i + base)
  }
  c <- base
}

func generate_seq(seq []Pair, n_threads int) {
  n := len(seq)
  ch := make(chan int)
  // Carve up the slice into sub-slices
  slice_len := updiv(n, n_threads)
  for i :=0; i < n_threads ; i++ {
    start := slice_len * i
    end := min(start + slice_len, n)
    go fill(seq[start:end], start, ch)
  }
  // Wait for all goroutines to finish
  for i := 0; i < n_threads; i++ {
    <- ch
  }
  close(ch)
}

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
    bidx := sort.SearchFloat64s(bucket_walls, in[i].x)
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

type ByX []Pair

func (s ByX) Len() int {
  return len(s)
}

func (s ByX) Swap(i, j int) {
  s[i], s[j] = s[j], s[i]
}

func (s ByX) Less (i, j int) bool {
  return s[i].x < s[j].x
}

func sequential_sort(seq []Pair, done chan bool) {
  sort.Sort(ByX(seq))
  done <- true
}

func verify_partition(bucket_walls []float64, bucket_offsets []int, bucket_counts []int, output []Pair) {
  n_buckets := len(bucket_offsets)
  for i:=0;i<n_buckets;i++ {
    start := 0.0
    if i > 0 {
      start = bucket_walls[i-1]
    }
    end := math.MaxFloat64
    if i < n_buckets - 1 {
      end = bucket_walls[i]
    }

    for j:=0;j<bucket_counts[i];j++ {
      cur_elem := output[bucket_offsets[i] + j]
      // fmt.Printf("%.3v ", cur_elem.x)
      if cur_elem.x < start || cur_elem.x > end {
        fmt.Printf("Error!! %.3v not in [%.3v, %.3v]\n", cur_elem, start, end)
      }
    }
    // fmt.Println()
  }

}

func print_sequence(seq []Pair) {
  for i:=0;i<len(seq);i++ {
    fmt.Printf("%.3v ", seq[i].x)
  }
  fmt.Println()

}

func verify_sorted(seq []Pair){
  for i:=0;i+1<len(seq);i++ {
    if seq[i].x > seq[i+1].x {
      fmt.Printf("Error! Sequence not sorted (%.3v > %.3v)\n", seq[i].x, seq[i+1].x)
    }
  }
}

func sample_sort(input []Pair, output []Pair, n_threads int) {
  
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
  block_len := updiv(n, n_threads)
  for i:=0; i<n_blocks; i++ {
    start := block_len * i
    end := min(start + block_len, n)
    go count(input[start:end], bucket_walls, count_channel)
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

  // print_sequence(output)

  verify_sorted(output)

  total_elapsed := time.Since(time_begin)

  
  fmt.Printf("Time taken to draw samples: %s\n", sample_elapsed)
  fmt.Printf("Time taken to get bucket counts: %s\n", count_elapsed)
  fmt.Printf("Time taken to partition data: %s\n", partition_elapsed)
  fmt.Printf("Time taken to sort buckets: %s\n", sort_elapsed)
  fmt.Println()
  fmt.Printf("Total time for sample_sort: %s\n", total_elapsed)
}

func main() {
  // fmt.Printf("# of OS threads: %v\n", runtime.GOMAXPROCS(0))

  n, n_threads, rounds := read_cmdline_input(os.Args)

  input := make([]Pair, n)
  output := make([]Pair, n)

  // Fill in the sequence with the hash values
  time_begin := time.Now()
  generate_seq(input, n_threads)
  elapsed := time.Since(time_begin)

  fmt.Printf("Time taken to generate input: %s\n", elapsed)

  // print_sequence(input)

  // Sort the sequence!
  for i:=0;i<rounds;i++ {
    fmt.Printf("\n------Round %d------\n", i+1)
    sample_sort(input, output, n_threads * 8)
  }
}