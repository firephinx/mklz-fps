#include <thrust/host_vector.h>
#include <thrust/device_vector.h>
#include <thrust/generate.h>
#include <thrust/copy.h>
#include <algorithm>
#include <cstdlib>
#include <iostream>
#include <numeric>
#include <ctime>

struct HashGenerator {
  int current_;
  HashGenerator (int start) : current_(start) {}
  double operator() () { current_++; 
                      return generateReal(current_);}
  // generates a int hash from an int value 
  int hash(int i)
  {
    uint64_t v = ((uint64_t) i) * 3935559000370003845 + 2691343689449507681;
    v = v ^ (v >> 21);
    v = v ^ (v << 37);
    v = v ^ (v >> 4);
    v = v * 4768777513237032717;
    v = v ^ (v << 20);
    v = v ^ (v >> 41);
    v = v ^ (v <<  5);
    return (int) (v & ((((uint64_t) 1) << 31) - 1));
  }

  // generates a pseudorandom double precision real from an integer
  double generateReal(int i) {
      return (double(hash(i)));
  }
};

struct IncGenerator {
    double current_;
    IncGenerator (double start) : current_(start) {}
    double operator() () { return current_++; }
};

void parallel_sort(){
  
}

int main(void)
{
  int N = 10000000;
  // generate 100M numbers serially
  thrust::host_vector<double> h_vec(N);
  thrust::host_vector<double> v_vec(N);
  thrust::host_vector<double> h_vec_result(N);
  thrust::host_vector<double> v_vec_result(N);
  HashGenerator HG (0);
  IncGenerator IG (0);

  clock_t begin_generation = clock();
  std::generate(h_vec.begin(), h_vec.end(), HG);
  std::generate(v_vec.begin(), v_vec.end(), IG);
  clock_t end_generation = clock();
  double generation_time = double(end_generation - begin_generation) / CLOCKS_PER_SEC;
  std::cout << "Generation Time: " << generation_time << std::endl;

  int numRuns = 5;

  for(int i = 0; i < numRuns; i++) {
    
    clock_t begin_sort_copy = clock();
    // transfer data to the device
    thrust::device_vector<double> d_vec = h_vec;
    thrust::device_vector<double> dv_vec = v_vec;

    clock_t begin_sort = clock();
    // sort data on the device
    prallel_sort(thrust::device, d_vec.begin(), d_vec.end(), dv_vec.begin());
    cudaThreadSynchronize();
    clock_t end_sort = clock();

    // transfer data back to host
    thrust::copy(d_vec.begin(), d_vec.end(), h_vec_result.begin());
    thrust::copy(dv_vec.begin(), dv_vec.end(), v_vec_result.begin());
    cudaThreadSynchronize();
    clock_t end_sort_copy = clock();

    double sort_copy_time = double(end_sort_copy - begin_sort_copy) / CLOCKS_PER_SEC;
    double sort_time = double(end_sort - begin_sort) / CLOCKS_PER_SEC;
    std::cout << "Sort + Copy Time: " << sort_copy_time << std::endl;
    std::cout << "Sort Only Time: " << sort_time << std::endl;

    clock_t begin_check = clock();
    for(int j = 1; j < N; j++) {
      if(h_vec_result[j] < h_vec_result[j-1]){
        std::cout << "Error: " << h_vec_result[j-1] << " is before " << h_vec_result[j] << std::endl;
      }
    }
    clock_t end_check = clock();
    double check_time = double(end_check - begin_check) / CLOCKS_PER_SEC;
    std::cout << "Check Time: " << check_time << std::endl;
  }

  return 0;
}