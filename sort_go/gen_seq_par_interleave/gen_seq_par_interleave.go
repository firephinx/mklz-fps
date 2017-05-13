package main

import (
  "os"
  "fmt"
  "strconv"
  "time"
  "runtime"
)

const expected_args int = 2

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

func timeTrack(start time.Time, name string) {
    elapsed := time.Since(start)
    fmt.Printf("%s took %s\n", name, elapsed)
}

func fill(a []Pair, base int) {
  for i := 0; i < len(a); i++ {
    a[i].x = float64(hash64(uint64(i + base)))
    a[i].y = float64(i + base)
  }
}

func worker(id, nthreads int, seq []Pair, c chan int) {
  const line_size = 64
  stride := nthreads * line_size

  for i:=id*line_size;i<len(seq);i+=stride {
    fill(seq[i:min(i+stride,len(seq))],i)
  }

  c <- id
}

func main() {
  if num_args := len(os.Args) ; num_args != expected_args + 1 {
    fmt.Printf("Usage: %s <n> <n_threads>\n", os.Args[0])
    os.Exit(1);
  }

  // How long a sequence to generate?
  n, err := strconv.Atoi(os.Args[1])
  if err != nil {
      fmt.Println(err)
      os.Exit(1)
  }

  // How many threads (goroutines) to use?
  n_threads, err := strconv.Atoi(os.Args[2])
  if err != nil {
    fmt.Println(err)
    os.Exit(1)
  }

  // Allocate a slice (backed by an array)
  seq := make([]Pair, n)

  time_begin := time.Now()

  ch := make(chan int)

  // Spawn a static thread pool
  for i:=0;i<n_threads;i++ {
    go worker(i, n_threads, seq, ch)
  }
  
  // Wait for all goroutines to finish
  for i := 0; i < n_threads; i++ {
    fmt.Printf("%v done\n", <- ch)
  }

  elapsed := time.Since(time_begin)

  fmt.Printf("Time taken to generate: %s\n", elapsed)

  fmt.Println(runtime.GOMAXPROCS(0))
  fmt.Println(runtime.NumCPU())

  // fmt.Println(seq)
}