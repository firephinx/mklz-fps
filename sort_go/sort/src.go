package main

import (
  "os"
  "fmt"
  "strconv"
  "time"
  // "runtime"
  "sort"
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

func read_cmdline_input(args []string) (int, int) {
  const expected_args int = 2

  if num_args := len(args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <n_threads>\n", args[0])
    os.Exit(1);
  }

  // How long a sequence to generate?
  n, err := strconv.Atoi(args[1])
  if err != nil {
      fmt.Println(err)
      os.Exit(1)
  }

  // How many threads (goroutines) to use?
  n_threads, err := strconv.Atoi(args[2])
  if err != nil {
    fmt.Println(err)
    os.Exit(1)
  }

  return n, n_threads
}

func fill(a []Pair, base int, c chan int) {
  for i := 0; i < len(a); i++ {
    a[i].x = float64(hash64(uint64(i + base)))
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

func sample_sort(seq []Pair, n_threads int) {
  n := len(seq)
  n_buckets := n_threads * 2
  oversample_stride := 2
  n_oversample := n_buckets * oversample_stride

  // Oversample the sequence (sequential)
  oversamples := make([]float64, n_oversample)
  for i := 0; i < n_oversample; i++ {
    random_index := hash64(uint64(i)) % uint64(n)
    oversamples[i] = seq[random_index].x
  }

  // Sort the oversamples
  sort.Sort(sort.Float64Slice(oversamples))

  // Get the actual bucket start points

  bucket_starts := make([]float64, n_buckets)
  for i:=0; i<n_buckets;i++ {
    bucket_starts[i] = oversamples[i * oversample_stride]
  }

  fmt.Println(bucket_starts)

  // In parallel, count how many elements are in each bucket
  
  // 

}

func main() {
  // fmt.Printf("# of OS threads: %v\n", runtime.GOMAXPROCS(0))

  n, n_threads := read_cmdline_input(os.Args)

  // Allocate a slice (backed by an array)
  seq := make([]Pair, n)

  // Fill in the sequence with the hash values
  time_begin := time.Now()
  generate_seq(seq, n_threads)
  elapsed := time.Since(time_begin)

  fmt.Printf("Time taken to generate: %s\n", elapsed)

  // Sort the sequence!
  sample_sort(seq, n_threads)
}