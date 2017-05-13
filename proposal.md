
# Project Proposal

## Summary

We are planning on competing in [15-210's Sorting Competition](http://www.cs.cmu.edu/~15210/sort.html) by Professor Guy Blelloch. In order to compete effectively, we are going to implement different parallel comparison-based sorts in a few languages and optimize for performance on the provided 72-core machine as well as run tests on a variety of other machine such as the unix machines, gates machines, latedays cluster, and Xeon Phis.

## Background

Sorting is a classical problem in computer science. The challenge with sorting these days is correctly optimizing the performance of different sorting algorithms on increasingly parallel and distributed machines. 

This parallel sorting competition was staged by Professor Guy Blelloch and the rest of the 210 team to incentivize students to help work on this real-world problem by writing efficient parallel sorting algorithms in a variety of languages, and especially garbage-collected languages. In addition, they would like to test how their SML work-stealing scheduler holds up against highly optimized solutions from Carnegie Mellon students.

## The Challenge

The main source of the challenge will be managing memory access and bandwidth requirements and creating a way for the sorting algorithm to adapt quickly and easily to each machine's configuration. Sorting is a problem with low arithmetic intensity; thus, making intelligent design choices based on the architecture of the target machine (e.g. caches, hyperthreading, etc.) will be crucial to getting good performance. 

We will also be coding in higher-level languages which, unlike C, will probably not expose as much low-level control over the machine to us. We will need to make use of our 418 knowledge to coax these high-level languages into giving us good performance.

## Resources

We will do initial testing on the 20 core unix machines and the Gates machines. 

Our “starter code” consists of the two reference implementations that Blelloch’s team provided. We will need to modify the given C++ code or install gcc 4.9 in order to get it to run on the GHC machines though. 

There is also substantial existing literature on parallel sorting algorithms. We will refer to those such as this paper on [Robust Massively Parallel Sorting](http://epubs.siam.org/doi/pdf/10.1137/1.9781611974768.7).

We will need access to the Xeon Phi machines in addition to the four days’ worth of Aware access that Blelloch’s team will give us.

## Goals and Deliverables

### Plan to achieve
* Analyse the performance of the C++ solution that they provided - why does it perform the way it does? Has it approached theoretical peak performance, and if not, what is slowing it down?
* Implement a semi-optimized parallel sorting algorithms in at least 2 garbage-collected languages (currently thinking Java, Haskell and Go) and analyse their performance. This will give us a rough idea of how the various languages compare in terms of:
  * Raw performance
  * How much control the languages provide the programmer over the low-level performance-determining details.
* Implement a highly optimized sorting algorithm on one chosen language. This would constitute our main submission to the sorting contest.
* The concrete goal is to make it run faster than their SML solution.

### Hope to achieve
* Implement an automated way of searching the parameter space (autotuning). This will allow us to make good use of the 4 days we have on the Aware machine.
* Highly optimize for more than one language.
* Implement a parallel sort in CUDA too.

### Planned Demo
* Performance comparison graphs between our optimized code and the baseline on different machines.

## Platform Choice

The platform has been already been determined for us by the rules of the contest, but we will choose a couple of the programming languages that we will use such as C++, Go, Java, Haskell, and possibly others depending on how well they are able to support SIMD instructions on Intel's chips.

We may also try getting a version to work on CUDA as stated in our stretch goals because sorting is a bandwidth-intensive problem, and GPUs have much higher bandwidth currently compared to CPUs.

## Schedule

| Week            | Tasks         |
| --------------- | ------------- |
| Apr 10 - Apr 16 | Review literature on parallel sorting algorithms <br/> Get the provided C++ code to compile and run on GHC machines <br/> Analyse the performance of the provided C++ code <br/> Choose at least 2 garbage-collected languages and get them working on the GHC machines (set up the environment so we are able to compile and run parallel code) |
| Apr 17 - Apr 23 | Implement first attempt at parallel sorting in the garbage-collected languages of our choice <br/> Analyse initial results from the first attempt and choose a language to focus on for our main contest submission <br/> Instrument code, use profiling tools, etc. to obtain deeper insight into the performance of the code we wrote in the previous week <br/> Submit our progress so far to Guy Blelloch’s team in order to request access to Aware machine |
| Apr 24 - Apr 30 | Do write-up for project checkpoint (due Apr 25) <br/> Focus on optimizing the parallel sort in the language of our choice <br/> Test and optimize on the Aware machine | 
| Apr 31 - May 5  | Optimize more <br/> Submit to the 210 sorting competition (due May 4) |
| May 6 - May 12  | Prepare for final presentation |
