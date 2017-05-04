package main

import (
  "sort"
)

func parallel_sample_sort(in, out ElementSlice, tweak ParamStruct) {
  for i:=0;i<len(in);i++ {
    out[i] = in[i]
  }
  sort.Sort(out)
}

// Partition strategy

// Sample.
// Choose n_blocks dividers
// Count how many elements fall within each type of divider
//   - This is done by binary searching on the dividers

// Write this to an array, and return.