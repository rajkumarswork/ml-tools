package ml.tools

import org.scalatest.flatspec.AnyFlatSpec
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers

import java.io._

class XGBTest extends AnyFlatSpec with Matchers with MockFactory {

  def fixture = new {
    val fields = Map("id" -> true, "gender" -> true, "height" -> false, "weight" -> false)
    val rows = Seq(
      Map("id" -> "1", "gender" -> "male", "height" -> "69", "weight" -> "165"),
      Map("id" -> "2", "gender" -> "female", "height" -> "63", "weight" -> "105"),
      Map("id" -> "3", "gender" -> "male", "height" -> "70", "weight" -> "160"),
      Map("id" -> "4", "gender" -> "female", "height" -> "64", "weight" -> "125"),
      Map("id" -> "5", "gender" -> "male", "height" -> "70", "weight" -> "180"))

    val label = "gender"
    val hashSize = 10
    val encoder = new SimpleEncoder(hashSize)

    val specialFields = Set(label, "weight")
    val lps = rows.map(r => HashTranslate.toLabeledPoint(r, fields, label, encoder))

    val xgb = new XGB(fields, label, hashSize)
  }

  "XGB" should "train OK" in {
    val f = fixture

    f.xgb.train(f.lps.iterator)
    f.xgb.trained() shouldBe true
  }
  it should "save and reload OK" in {
    val f = fixture
    f.xgb.train(f.lps.iterator)

    val mos = new ByteArrayOutputStream()
    val ios = new ByteArrayOutputStream()
    f.xgb.saveModel(mos)
    f.xgb.saveInfo(ios)

    val mis = new ByteArrayInputStream(mos.toByteArray())
    val iis = new ByteArrayInputStream(ios.toByteArray())
    val model = f.xgb.loadModel(mis)
    val info = f.xgb.loadInfo(iis).get
    assert(info == f.xgb.info && model != null)
  }
}
