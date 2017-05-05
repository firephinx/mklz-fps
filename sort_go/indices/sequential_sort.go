package main

import "sort"

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

func sequential_sort_by_index(seq ElementSlice, done chan bool) {
  // The list to be sorted
  n := len(seq)
  idx := make([]int32, n)
  for i:=0;i<n;i++ {
    idx[i] = int32(i)
  }

  // Sort indices by their elements
  sort.Slice(idx, func(i, j int) bool { return (&seq[i]).Less(&seq[j]) })

  // Sort the bloody things. First pass: naive

  tmp := make(ElementSlice, n)

  for i:=0;i<n;i++ {
    tmp[i] = seq[idx[i]]
  }

  for i:=0;i<n;i++ {
    seq[i] = tmp[i]
  }
  done <-true
}