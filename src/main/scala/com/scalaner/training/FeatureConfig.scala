package com.scalaner.training


object FeatureConfig extends Enumeration {

  val DEFAULT = IndexedSeq(
    ("maxLeft", 1),
    ("useClassFeature", true),
    ("useWord", true),
    ("useNGrams", true),
    ("noMidNGrams", true),
    ("maxNGramLeng", 6),
    ("usePrev", true),
    ("useNext", true),
    ("useDisjunctive", true),
    ("useSequences", true),
    ("usePrevSequences", true),
    ("useTypeSeqs", true),
    ("useTypeSeqs2", true),
    ("useTypeySequences", true)
  )

}
