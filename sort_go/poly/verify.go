// This file contains functions for verifying that the sort worked
// It assumes that the elements to be sorted are Pairs, so that it can
// check the y-values.

package main

import "fmt"

func verify_output_pairs(seq []Pair) bool {
  // Maintain an array of flags that indicates whether each element is present
  flags := make([]bool, len(seq))

  // Verify that the sequence is sorted in one pass
  for i:=0;i+1<len(seq);i++ {
    // Check for order
    if seq[i].Less(&seq[i+1]) == false {
      fmt.Println("Error: not in sorted order!")
      fmt.Println("Offending elements:")
      seq[i].Print()
      seq[i+1].Print()

      return false
    }

    // Check for uniqueness
    flags[uint64(seq[i].y)] = true
    if flags[uint64(seq[i+1].y)] {
      fmt.Println("Error: not unique!")
      fmt.Println("Offending element:")
      seq[i+1].Print()

      return false
    }
  }

  fmt.Println("Sequence was verified to be sorted")

  return true
}