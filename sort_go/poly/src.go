package main

import (
  "fmt"
  "sort"
)

// Here you can define the type of the element to be sorted.
type ElementType Pair
type ElementSlice []Pair

func main() {
  n := 10

  fmt.Printf("%d elements\n", n)

  input := make(ElementSlice, n)

  for i:=0;i<n;i++ {
    input[i].Generate(i)
    input[i].Print()
  }

  sort.Sort(input)

  fmt.Printf("Sorted\n")

  for i:=0;i<n;i++ {
    input[i].Print()
  }
  verify_output_pairs(input)
}