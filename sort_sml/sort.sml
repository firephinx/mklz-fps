structure Main =
struct
  structure Seq = ArraySequence
  structure Args = CommandLineArgs
  structure A = ArrayExtra : ARRAY_EXTRA
  structure AS = ArraySliceExtra : ARRAY_SLICE_EXTRA

  fun getTime f = let
      val startTime = Time.now ()
      val result = f ()
      val endTime = Time.now ()
      val elapsedTime = Time.- (endTime, startTime)
    in (result, elapsedTime)
    end

  fun timeStr t = 
     String.concat ["wall-elapsed: ", Time.fmt 4 t, "\n"]

  fun for (lo, hi) f = 
    if (lo >= hi) then () else (f(lo); for (lo+1,hi) f)

  fun for_l (lo, len) f = for (lo, lo + len) f

  fun parallel_for granularity (lo, hi) f =
    if (hi - lo) <= granularity then for (lo,hi) f
    else let val mid = lo + ((hi-lo) div 2)
         in Primitives.par (fn () => parallel_for granularity (lo, mid) f,
                            fn () => parallel_for granularity (mid, hi) f); ()
         end

  fun parallel_tabulate granularity f n = 
    let val R = A.arrayUninit n
    in (parallel_for granularity (0,n) (fn i => A.update(R,i,f(i))); 
        AS.full(R)) 
    end

   fun check_sorted cmp s = 
     let val n = Seq.length s
         val x = Seq.tabulate (fn i => not(cmp(Seq.nth s i, Seq.nth s (i+1)) = GREATER)) (n - 1)
     in Seq.reduce (fn (a,b) => a andalso b) true x end

  fun time_samplesort_pair n = let 
        val a = parallel_tabulate 10000 (fn i => (generateReal(i),
                                                  Real64.fromInt(i))) n
        val cmp = fn ((ak,av),(bk,bv)) => Real64.compare(ak,bk)
        val (r,time) = getTime(fn () => SampleSort.sort cmp a)
        val _ = if not(check_sorted cmp r) then print("Not sorted in Sample Sort\n") else ()
    in time end

  fun runTest (k,n) (name,f) =
    let val _ = print (String.concat ["\n", name, "\n"])
        fun r(i) = if (i > k) then ()
                   else (print ("trial " ^ Int.toString i ^ ": " ^ timeStr(f n)); 
			 r(i+1))
    in r(1) end
         
  fun run () =
    let
      val _ = Args.init ()
      val n_default = 1000000    
      val n = Args.parseOrDefaultInt ("n", n_default)
      val rounds = Args.parseOrDefaultInt ("r", 1)
      val init = if n > 100000000 then 1 else 4

      (* run a small test to initialize the scheduler *)
      val _ = for (0,init) (fn _ => time_samplesort_pair n)

      (* now run main tests *)
      val _ = runTest (rounds, n) ("SampleSort Pairs of Doubles", time_samplesort_pair)
   in () end
end

val () = Main.run ()
