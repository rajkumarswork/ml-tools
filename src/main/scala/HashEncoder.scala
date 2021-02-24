package ml.tools

import com.spotify.featran._
import com.spotify.featran.transformers._
import scala.util.hashing.MurmurHash3

// Encoder that hash-encodes a list-of-strings to a float-array of size hashSize
trait Encoder {
  def encode(strings: Seq[String]): Seq[Float];
}

// - Simple Encoder --
class SimpleEncoder(hashSize: Int) extends Encoder {
  def encode(strings: Seq[String]): Seq[Float] = HashEncoder.hashEncode(strings, hashSize)
}

// - Featran Encoder --
class FeatranEncoder(hashSize: Int) extends Encoder {

  def encode(strings: Seq[String]): Seq[Float] = extractor.featureResult(Rec(strings)).value

  // - privates --
  private case class Rec(fs: Seq[String])

  private val spec: FeatureSpec[Rec] =
    FeatureSpec.of[Rec].required(_.fs)(HashNHotEncoder("Hash", hashSize))

  private val settings: String = spec.extract(Seq(Rec(Seq[String]()))).featureSettings.head

  private val extractor: RecordExtractor[Rec, Seq[Float]] =
    spec.extractWithSettings[Seq[Float]](settings)
}

// Tools to estimate hash-size
object HashEncoder {

  // Encoder used by Featran
  def bucket(x: String, c: Int): Int = (MurmurHash3.stringHash(x) % c + c) % c

  def hashEncode(xs: Seq[String], c: Int): Array[Float] = {
    val a = new Array[Float](c)
    xs.foreach(x => a(bucket(x, c)) = 1)
    a
  }

  // HyperLogLog estimate of distinct tokens in dataset
  def distinctTokens(rit: Iterator[Seq[String]]): Long = {
    val hll = new com.twitter.algebird.HyperLogLogMonoid(16)
    val sumHll = hll.sum(rit.flatten.map(_.getBytes).map(hll.toHLL(_)))
    hll.sizeOf(sumHll).estimate
  }
  // suggested hash-size given number of tokens (see above) and max collision probability
  // p(collision) ~ k^2 / 2N (k -> tokens, N -> hash-capacity)
  def minHashSize(tokens: Long, collisionRate: Float = 0.1f): Int =
    Math.ceil(Math.log(tokens * tokens / (2 * collisionRate)) / Math.log(2)).toInt
}
