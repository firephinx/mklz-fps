#include <stdio.h>
#include <stdint.h>

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

int main(){
  printf("%llu\n", hash64(0));

  return 0;
}

