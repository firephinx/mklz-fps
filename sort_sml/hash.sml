(* Uses a hash function from Numerical Recipes *)

(* The C Code 
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
    return (int) (v & (((uint64_t 1) << 31) - 1))
  }

  // generates a pseudorandom double precision real from an integer
  double generateReal(int i) {
      return (double(hash(i));
  }
*)
(* The SML Code *)

  fun hash(i) = let
     open Word64
     infix 2 >> infix 2 << infix 2 xorb infix 2 andb
     val v = fromInt(i) * 0w3935559000370003845 + 0w2691343689449507681
     val v = v xorb (v << 0w21)
     val v = v xorb (v << 0w37)
     val v = v xorb (v >> 0w4)
     val v = v * 0w4768777513237032717
     val v = v xorb (v << 0w20)
     val v = v xorb (v >> 0w41)
     val v = v xorb (v << 0w5)
   in Word64.toInt (v andb ((0w1 << 0w31) - 0w1)) end

  fun generateReal(i) = Real64.fromInt(hash(i))


