package main

import (
  "os"
  "fmt"
  "time"
  "runtime"
  "runtime/trace"
  "flag"
)

type ParamStruct struct {
  n, threads, rounds int
  n_buckets, oversample_stride, n_countblocks int
  sort_by_indices bool
  enable_trace bool
  verbose bool
  verify_output bool
}

func read_flags() ParamStruct {
  var ps ParamStruct
  default_threads := runtime.NumCPU()

  flag.IntVar(&ps.n, "n", 1000000, "Number of elements to sort")
  flag.IntVar(&ps.threads, "t", default_threads * 4, "Number of threads")
  flag.IntVar(&ps.rounds, "r", 1, "How many rounds to run")
  flag.IntVar(&ps.n_buckets, "buckets", default_threads * 8, "Number of buckets")
  flag.IntVar(&ps.oversample_stride, "oversample", 4, "Oversample stride")
  flag.IntVar(&ps.n_countblocks, "countblocks", default_threads * 4, "Number of countblocks")
  flag.BoolVar(&ps.sort_by_indices, "i", false, "Sort by indices?")
  flag.BoolVar(&ps.enable_trace, "trace", false, "Enable trace?")
  flag.BoolVar(&ps.verbose, "v", false, "Verbose?")
  flag.BoolVar(&ps.verify_output, "verify", false, "Verify output?")

  flag.Parse()

  return ps
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
    true, // sort by indices
    false, // enable trace
    true, // verbose
    true, // verify output
  }
}

func main() {
// Read cmdline input
  // ps := read_cmdline_input_struct(os.Args)  
  ps := read_flags()  

// Boilerplate to enable go trace tool
  if ps.enable_trace {
    f, err := os.Create("trace.out")
    if err != nil {
      panic(err)
    }
    defer f.Close()

    err = trace.Start(f)
    if err != nil {
      panic(err)
    }
    defer trace.Stop()
  }

  n, threads, rounds := ps.n, ps.threads, ps.rounds
  // n, threads, rounds := read_cmdline_input(os.Args)
  if ps.verbose {
    fmt.Printf("%d elements, %d threads, %d rounds\n", n, threads, rounds)
  }
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
  if ps.verbose {
    fmt.Printf("Generating input: %s\n", elapsed_generate)
    fmt.Println("\n===== Doing a few warm-up sorts =====")
  }

  for r:=0;r<5;r++ {
    parallel_sample_sort(input, output, ps)    
  }

// Sort #rounds times
  if ps.verbose {
    fmt.Println("\n===== Actual sorting begins =====")
  }

  for r:=0;r<rounds;r++ {
    if ps.verbose {
      fmt.Printf("Round %v: \n", r)
    }

    // This is where our sort function should be called from!
    time_sort := time.Now()
    // sequential_sort_copy(input, output)
    parallel_sample_sort(input, output, ps)
    // old_sample_sort(input, output, ps)
    elapsed_sort := time.Since(time_sort)

    // Do some simple book-keeping
    if ps.verbose {
      fmt.Printf("%s\n", elapsed_sort)
      runtimes[r] = elapsed_sort.Seconds()
    }

    // Verify that the output produced was correct
    if ps.verify_output {
      verify(output)
    }
    if ps.verbose {
      fmt.Println() 
    }
  }

// Print the best running-time
  best_time := runtimes[0]
  for _,t := range(runtimes) {
    if t < best_time { best_time = t }
  }

  if ps.verbose {
    fmt.Printf("Best time: %.3fs\n", best_time)
  }
}
