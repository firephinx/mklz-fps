package main

import "sort"
// Define sorting interface for a slice of our custom elements
// type ElementSlice []Pair

func (s ElementSlice) Less (i, j int) bool {
  return (&s[i]).Less(&s[j])
}

func (s ElementSlice) Len() int {
  return len(s)
}

func (s ElementSlice) Swap(i, j int) {
  s[i], s[j] = s[j], s[i]
}

// A function suitable to be used as a goroutine
func sequential_sort(seq ElementSlice, done chan bool) {
  sort.Sort(seq)
  done <- true
}

func sequential_sort_copy(input, output ElementSlice) {
  for i:=0;i<len(input);i++ {
    output[i] = input[i]
  }
  sort.Sort(output)
}