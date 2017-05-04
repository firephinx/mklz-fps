package main

import (
  "os"
  "fmt"
  "strconv"
)

func min(x,y int) int {
  if x < y {
    return x
  }
  return y
}

func updiv(x, y int) int {
  return (x + y - 1) / y
}

func block(seq ElementSlice, i, s int) ElementSlice {
  n := len(seq)
  return seq[i*s : min((i+1)*s, n)]
}

func string_to_int(s string) int {
  x, err := strconv.Atoi(s)
  if err != nil {
      fmt.Println(err)
      os.Exit(1)
  }
  return x
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

func barrier(done chan bool, n int) {
  for i:=0;i<n;i++ { <-done }
}