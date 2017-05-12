#include <stdio.h>
#include <algorithm>
#include "get_time.h"
#include <stdint.h>
using namespace std;

#define N 10000000

struct elem {
  double x;
  double y;
} input[N], output[N];

// from numerical recipes
uint64_t hash64(uint64_t u )
{
  uint64_t v = u * 3935559000370003845 + 2691343689449507681;
  v ^= v >> 21;
  v ^= v << 37;
  v ^= v >>  4;
  v *= 4768777513237032717;
  v ^= v << 20;
  v ^= v >> 41;
  v ^= v <<  5;
  return v;
}

timer bt;

bool cmp(const elem &a, const elem& b){
  return a.x < b.x;
}

int main(){
  for(int i=0;i<N;i++) {
    input[i].x = (double) hash64((uint64_t) i);
    input[i].y = (double) i;
  }

  // Begin timing
  bt.start();

  for(int i=0;i<N;i++){
    output[i] = input[i];
  }
 
  // Sort
  sort(output, output+N, cmp);

  // End timing
  double elapsed = bt.stop();
  printf("Elapsed: %.3lf\n", elapsed);

}