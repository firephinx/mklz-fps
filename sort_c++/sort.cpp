#include "utilities.h"
#include "get_time.h"
#include "sequence_ops.h"
#include "sample_sort.h"
#include <cilk/cilk.h>
#include <cilk/cilk_api.h>
#include <iostream>
#include <ctype.h>
#include <math.h>
#include <limits>
#include <vector>
#include <algorithm>

static timer bt;
using namespace std;

#define time(_var,_body)    \
  bt.start();               \
  _body;		    \
  double _var = bt.stop();

size_t str_to_int(char* str) {
    return strtol(str, NULL, 10);
}

double t_sort_pair(size_t n) {
  using p = pair<double, double>;
  p* R;
  sequence<p> In(n, [&] (size_t i) {
      return p((double) (pbbs::hash64(i) & ((((long) 1) << 31) - 1)), 
	       (double) i);});
  auto cmp = [&] (p& a, p& b) {return a.first < b.first;};
  time(t, R = pbbs::sample_sort(In.as_array(), n, cmp););
  sequence<bool> check(n, false);

  parallel_for (size_t i = 0; i < n; i++) {
    check[(long) R[i].second] = true;
    if (i > 0 && cmp(R[i], R[i-1])) {cout << "out of order at: " << i << endl; abort();}
  }

  // check that all pairs are present
  parallel_for (size_t i = 0; i < n; i++) {
    if (!check[i]) {cout << "missing element: " << i << endl; abort();}
  }
  free(R);
  return t;
}

template<typename F>
void run_multiple(size_t n, size_t rounds, string name, F test) {
  cout << name << endl;
  // run a few for warm up
  for (size_t i=0; i < 2; i++) test(n);

  for (size_t i=0; i < rounds; i++) {
    double t = test(n);
    cout << std::setprecision(3) << "trial " << i << " wall-elapsed: " << t << endl;
  }
}

int main (int argc, char *argv[]) {
  if (argc > 4) {
    fprintf(stderr, "sort <n> <rounds>\n");
    exit(1);
  }
  size_t n       = str_to_int(argv[1]);
  size_t rounds  = str_to_int(argv[2]);

  run_multiple(n, rounds, "C++: Sample Sort Pairs of Doubles", t_sort_pair);
}
  
  

