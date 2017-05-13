#include <stdio.h>

int updiv(int x, int y){
  return (x+y-1) / y;
}

int main(){
  int n;
  scanf("%d", &n);

  for(int i=1;i<=2*n;i++){
    printf("%d / %d : %d\n", n, i, updiv(n,i));
  }

  return 0;
}