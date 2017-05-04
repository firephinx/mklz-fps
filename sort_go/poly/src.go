package main

import (
  "os"
  "fmt"
  "time"
)

func read_cmdline_input(args []string) (int, int, int) {
  const expected_args int = 3
  if num_args := len(args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <threads> <rounds\n", args[0])
    os.Exit(1);
  }
  return string_to_int(args[1]), string_to_int(args[2]), string_to_int(args[3])
}

func main() {
// Read cmdline input
  n, threads, rounds := read_cmdline_input(os.Args)
  fmt.Printf("%d elements, %d threads, %d rounds\n", n, threads, rounds)
  if rounds == 0 { os.Exit(0) } // quit now if 0 rounds requested

// Allocate input and output slices
  output := make(ElementSlice, n)
  input := make(ElementSlice, n)

// Generate input sequence
  time_generate := time.Now()
  
  done := make(chan bool, threads)
  stride := updiv(n, threads)
  for i:=0;i<threads;i++ {
    go func (i int) {
      blk := block(input, i, stride)
      base := i * stride
      for j := range blk {
        blk[j].Generate(j + base)
      }
      done <- true
    }(i)
  }
  barrier(done, threads)

  elapsed_generate := time.Since(time_generate)
  fmt.Printf("Generating input: %s\n", elapsed_generate)

// Find best running time
  runtimes := make([]float64, rounds)

// Sort #rounds times
  for r:=0;r<rounds;r++ {
    fmt.Printf("Round %v: ", r)

    // This is where our sort function should be called from!
    time_sort := time.Now()
    sequential_sort_copy(input, output)
    elapsed_sort := time.Since(time_sort)

    // Do some simple book-keeping
    fmt.Printf("%s\n", elapsed_sort)
    runtimes[r] = elapsed_sort.Seconds()

    // Verify that the output produced was correct
    verify(output)
  }

// Print the best running-time
  best_time := runtimes[0]
  for _,t := range(runtimes) {
    if t < best_time { best_time = t }
  }

  fmt.Printf("Best time: %.3fs\n", best_time)
}
