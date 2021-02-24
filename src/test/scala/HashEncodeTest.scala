package ml.tools

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HashEncodeTest extends AnyFlatSpec with Matchers {

  def fixture = new {
    val hashSize = 10
    val encoder = new SimpleEncoder(hashSize)
  }

  "encoder " should "hash-encode as expected" in {
    val f = fixture

    val strings = Seq("color:white", "shoe:leather", "country:us")
    val encodedArray = f.encoder.encode( strings )

    val expected = new Array[Float](f.hashSize)
    strings.foreach( s => expected( HashEncoder.bucket( s, f.hashSize ) ) = 1 )

    encodedArray should equal(expected)
  }

  it should "be idempotent" in {
    val f = fixture

    val string = Seq("color:white")
    val strings = Seq("color:white", "color:white")

    f.encoder.encode( strings ) should equal( f.encoder.encode( string ))
  }
}
