package main

import (
  "fmt"
  "math"
)
type Pair struct {
  x float64
  y float64
}

// Define "less-than" function for two elements
func (a *Pair) Less (b *Pair) bool {
  return a.x < b.x
}

// Generate the ith element
func (a *Pair) Generate(i int) {
  a.x = float64(hash64(uint64(i)))
  a.y = float64(i)
}

// Pretty-print :)
func (a Pair) Print() {
  fmt.Printf("(%.2f, %.0f)\n", math.Log10(a.x), a.y)
}
