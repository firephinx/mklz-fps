package main

import (
  "os"
  "fmt"
  "strconv"
)

const max_args int = 2

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
    fmt.Printf("Expected %d args, received %d\n", max_args, num_args)
    os.Exit(1);
  }

  fmt.Printf("Number of arguments: %d\n", len(os.Args))
  fmt.Println(os.Args)
  fmt.Println(os.Args[1])


  n, err := strconv.Atoi(os.Args[1])
  if err != nil {
      // handle error
      fmt.Println(err)
      os.Exit(2)
  }

  fmt.Println(n + 2)
  // fmt.Println(hash64(0))
}