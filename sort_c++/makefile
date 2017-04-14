CC = g++
CFLAGS = -O2 -Wall -fcilkplus -std=c++11

sort:	sort.cpp
	$(CC) $(CFLAGS) sort.cpp -o sort || (echo "requires g++ version 4.9 or greater"; exit 1)

clean:
	rm -f sort

