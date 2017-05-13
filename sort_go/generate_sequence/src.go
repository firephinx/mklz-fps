package main

import (
  "os"
  "fmt"
  "strconv"
)

const max_args int = 2

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

func main() {
  if num_args := len(os.Args) ; num_args < max_args {
    fmt.Printf("Usage: %s <n>\n", os.Args[0])
    os.Exit(1);
  }

  // How long a sequence to generate?
  n, err := strconv.Atoi(os.Args[1])
  if err != nil {
      fmt.Println(err)
      os.Exit(1)
  }

  // Allocate a slice (backed by an array)
  seq := make([]Pair, n)

  for i := 0; i < n; i++ {
    seq[i].x = float64(hash64(uint64(i)))
    seq[i].y = float64(i)
  }

  fmt.Println(seq)

  fmt.Println(len(seq))
  fmt.Println(cap(seq))
}