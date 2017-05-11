package main

import (
  "sort"
  "time"
  // "fmt"
)
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
// func sequential_sort(seq ElementSlice, done chan bool) {
//   sort.Sort(seq)
//   done <- true
// }

func sequential_sort(seq ElementSlice, info_channel chan seq_sort_info) {
  var info seq_sort_info
  info.n_elems = len(seq)

  time_begin := time.Now()
  sort.Sort(seq)
  info.total_duration = time.Since(time_begin) 
  info_channel <- info
}

func sequential_sort_copy(input, output ElementSlice) {
  for i:=0;i<len(input);i++ {
    output[i] = input[i]
  }
  sort.Sort(output)
}



// func sequential_sort_by_index(seq ElementSlice, done chan bool) {
func sequential_sort_by_index(seq ElementSlice, info_channel chan seq_sort_info) {
  var info seq_sort_info
  info.n_elems = len(seq)

  // The list to be sorted
  time_begin := time.Now()

  n := int32(len(seq))
  idx := make([]int32, n)
  for i:=int32(0);i<n;i++ {
    idx[i] = i
  }

  // Sort indices by their elements
  sort.Slice(idx, func(i, j int) bool { return (&seq[idx[i]]).Less(&seq[idx[j]]) })

  info.sort_duration = time.Since(time_begin)

  time_shuffle := time.Now()

  // First attempt: naive

  // tmp := make(ElementSlice, n)

  // for i:=int32(0);i<n;i++ {
  //   tmp[i] = seq[idx[i]]
  // }

  // for i:=int32(0);i<n;i++ {
  //   seq[i] = tmp[i]
  // }

  // Second pass: unravel the permutation

  for i:=int32(0);i<n;i++ {
    if idx[i] < n {
      tmp := seq[i] // hold in our hand the current element

      j := i
      for idx[j] != i {
        seq[j] = seq[idx[j]]
        next := idx[j]
        idx[j] = n
        j = next
      }
      seq[j] = tmp
      idx[j] = n
    }
  }

  info.shuffle_duration = time.Since(time_shuffle)

  info.total_duration = time.Since(time_begin)
  // done <-true
  info_channel <- info
}