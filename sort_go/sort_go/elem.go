package main

import (
  "fmt"
  "math"
)
type Pair struct {
  x float64
  y float64

  // a1 float64
  // a2 float64
  // a3 float64
  // a4 float64
  // a5 float64
  // a6 float64
  // a7 float64
  // a8 float64

  // a11 float64
  // a12 float64
  // a13 float64
  // a14 float64
  // a15 float64
  // a16 float64
  // a17 float64
  // a18 float64

  // a21 float64
  // a22 float64
  // a23 float64
  // a24 float64
  // a25 float64
  // a26 float64
  // a27 float64
  // a28 float64
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
