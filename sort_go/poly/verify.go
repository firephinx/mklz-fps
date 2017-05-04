// This file contains functions for verifying that the sort worked
// It assumes that the elements to be sorted are Pairs, so that it can
// check the y-values.

package main

import "fmt"

func verify_output_pairs(seq []Pair) bool {
  n := len(seq)
  if n == 0 {
    return true // Empty sequence is vacuously sorted
  }

  // Maintain an array of flags that indicates whether each element is present
  flags := make([]bool, n)

  // A counter to check the sum of the y-values
  sum := uint64(seq[0].y)
  expected_total := uint64(n) * uint64(n-1) / 2

  // Verify that the sequence is sorted in one pass
  for i:=0;i+1<n;i++ {
    // Check for order
    if seq[i].Less(&seq[i+1]) == false {
      fmt.Println("Error: not in sorted order!")
      fmt.Println("Offending elements:")
      seq[i].Print()
      seq[i+1].Print()

      return false
    }

    // Check for uniqueness of y-values of pairs in sorted output
    flags[uint64(seq[i].y)] = true // set the flag of the first element
    if flags[uint64(seq[i+1].y)] { // make sure second element is not seen yet
      fmt.Println("Error: not unique!")
      fmt.Println("Offending element:")
      seq[i+1].Print()

      return false
    }

    // Keep running sum of y-values
    sum += uint64(seq[i+1].y)
  }

  if sum != expected_total {
    fmt.Printf("Error: y-values did not sum to %v, got %v instead\n", expected_total, sum)
    return false
  }

  fmt.Println("Output sequence was verified :)")

  return true
}