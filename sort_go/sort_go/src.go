package main

import (
  "os"
  "fmt"
  "time"
)

type ParamStruct struct {
  n, threads, rounds int
  n_buckets, oversample_stride, n_countblocks int
}

func read_cmdline_input(args []string) (int, int, int) {
  const expected_args int = 3
  if num_args := len(args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <threads> <rounds\n", args[0])
    os.Exit(1);
  }
  return string_to_int(args[1]), string_to_int(args[2]), string_to_int(args[3])
}

func read_cmdline_input_struct(args []string) ParamStruct {
  const expected_args int = 3
  if num_args := len(args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <threads> <rounds\n", args[0])
    os.Exit(1);
  }

  n := string_to_int(args[1])
  threads := string_to_int(args[2])
  rounds := string_to_int(args[3])

  return ParamStruct{
    n,
    threads * 4,
    rounds,
    threads * 8, // # of buckets
    4,
    threads * 4, // # of countblocks
  }
}

func main() {
// Read cmdline input
  ps := read_cmdline_input_struct(os.Args)  
  n, threads, rounds := ps.n, ps.threads, ps.rounds
  // n, threads, rounds := read_cmdline_input(os.Args)
  fmt.Printf("%d elements, %d threads, %d rounds\n", n, threads, rounds)
  if rounds == 0 { os.Exit(0) } // quit now if 0 rounds requested

// Allocate slices
  output := make(ElementSlice, n)
  input := make(ElementSlice, n)
  runtimes := make([]float64, rounds)

// Generate input sequence
  time_generate := time.Now()
  
  done := make(chan bool, threads)
  for i:=0;i<threads;i++ {
    go func (i int) {
      blk, base := block(input, i, threads)
      for j := range blk {
        blk[j].Generate(j + base)
      }
      done <- true
    }(i)
  }
  barrier(done, threads)

  elapsed_generate := time.Since(time_generate)
  fmt.Printf("Generating input: %s\n", elapsed_generate)

  fmt.Println("\n===== Doing a few warm-up sorts =====")

  for r:=0;r<5;r++ {
    parallel_sample_sort(input, output, ps)    
  }

// Sort #rounds times
  fmt.Println("\n===== Actual sorting begins =====")

  for r:=0;r<rounds;r++ {
    fmt.Printf("Round %v: \n", r)

    // This is where our sort function should be called from!
    time_sort := time.Now()
    // sequential_sort_copy(input, output)
    parallel_sample_sort(input, output, ps)
    // old_sample_sort(input, output, ps)
    elapsed_sort := time.Since(time_sort)

    // Do some simple book-keeping
    fmt.Printf("%s\n", elapsed_sort)
    runtimes[r] = elapsed_sort.Seconds()

    // Verify that the output produced was correct
    verify(output)
    fmt.Println()
  }

// Print the best running-time
  best_time := runtimes[0]
  for _,t := range(runtimes) {
    if t < best_time { best_time = t }
  }

  fmt.Printf("Best time: %.3fs\n", best_time)
}
