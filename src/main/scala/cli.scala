package ml.tools

import org.rogach.scallop._
import io.circe.generic.auto._, io.circe.syntax._

// - command line parsing -------------------
class Conf(args: Array[String]) extends ScallopConf(args) {
  version("ml-tools 0.0.1, 2/21\n")
  banner("""Usage: ml-tools [export|test|train] options...
           |
           |Options:
           |""".stripMargin)

  object export extends Subcommand("export", "ex") {
    val format = choice(choices = Seq("svm", "lp", "tf"))
    val input = opt[String](required = true)
    val output = opt[String](required = true)
    val label = opt[String]()
    val hashsize = opt[Int]()
    validate(input) { i =>
      if (IO.tableExists(i)) Right(())
      else Left(s"Table $input not found")
    }
    validateOpt(format, label) {
      case (Some("svm"), None) => Left("Label required for svm format")
      case (Some("lp"), None) => Left("Label required for lp format")
      case _ => Right(())
    }
  }
  object train extends Subcommand("train", "tr") {
    val input = opt[String](required = true)
    val model = opt[String](required = true)
    val label = opt[String]()
    val hashsize = opt[Int]()
    validate(input) {
      case i: String if IO.sourceExists(i) => Right(())
      case t: String if IO.tableExists(t) && label.isDefined => Right(())
      case _ => Left(s"${input()} needs to be a valid file or valid table with label specified")
    }
  }
  object test extends Subcommand("test", "te") {
    val input = opt[String](required = true)
    val model = opt[String]()
    val label = opt[String]()
    val hashsize = opt[Int]()
    validate(model) {
      case m if IO.isValidModel(m) => Right(())
      case _ => Left(s"${model()} needs to be a valid XGB model")
    }
    validateOpt(input, label) {
      case (Some(f), None) if IO.isValidSource(f) => Right(())
      case (Some(t), None) if IO.isValidTable(t) => Left(s"Label reqired for table")
      case (Some(t), Some(_)) if IO.isValidTable(t) => Right(())
      case _ => Left(s"${input()} needs to be a valid svm source, or table with label")
    }
  }

  addSubcommand(export)
  addSubcommand(train)
  addSubcommand(test)

  verify()
}

// - Entry object -----------------------------------------
object cli {

  // - subcommands ----
  def export(c: Conf): Unit = {
    val x = c.export
    val f = x.format.toOption.getOrElse(fileFormat(x.output()))
    val os = IO.oStream(x.output())
    f match {
      case "lp" ⇒
        val hashSize = getHashSize(x.hashsize.toOption, x.input())
        HashTranslate.BQLabeledPoints(x.input(), x.label(), hashSize)
          .foreach(l ⇒ os.write((l.asJson.noSpaces + "\n").getBytes))
      case "svm" ⇒
        val hashSize = getHashSize(x.hashsize.toOption, x.input())
        HashTranslate.BQSvms(x.input(), x.label(), hashSize)
          .foreach(l ⇒ os.write((l + "\n").getBytes))
      case "tf" ⇒ BQ.tableExamples(x.input()).foreach(e ⇒ e.writeTo(os))
    }
    os.close
  }
  def train(c: Conf): Unit = {
    val r = c.train
    val m = r.input() match {
      case t: String if IO.isValidTable(t) && r.label.toOption.isDefined =>
        val hashSize = getHashSize(r.hashsize.toOption, t)
        XGB.train(HashTranslate.BQLabeledPoints(t, r.label(), hashSize))
      case o: String if IO.isValidBlob(o) ⇒ XGB.trainBlob(o)
      case f: String ⇒ XGB.trainFile(f)
    }
    XGB.saveModel(m, IO.oStream(r.model()))
  }
  def test(c: Conf): Unit = {
    val e = c.test
    val o = e.input() match {
      case t: String if IO.isValidTable(t) && e.label.toOption.isDefined =>
        val hashSize = getHashSize(e.hashsize.toOption, t)
        val lps = HashTranslate.BQLabeledPoints(t, e.label(), hashSize)
        if (e.model.toOption.isDefined) XGB.auc(XGB.loadModel(IO.iStream(e.model())), lps).toString
        else XGB.crossValidate(lps)
      case f: String if e.model.toOption.isDefined =>
        val model = XGB.loadModel(IO.iStream(e.model()))
        val lps = XGB.loadSamples(IO.iStream(f))
        XGB.auc(model, lps).toString
      case o: String if IO.isValidBlob(o) ⇒ XGB.crossValidateBlob(o)
      case f: String => XGB.crossValidateFile(f)
    }
    println(o)
  }

  // - privates and helpers --------------------------
  private def getHashSize(ho: Option[Int], table: String): Int = {
    val h = if (ho.isDefined) ho.get else (HashTranslate.minHashSize(table))
    println(s"Min hash-size: $h")
    h
  }
  private def fileFormat(s: String): String = s.toLowerCase match {
    case s: String if s.endsWith("lp") || s.endsWith("json") ⇒ "lp"
    case s: String if s.endsWith("tf") || s.endsWith("tfr") ⇒ "tf"
    case _ ⇒ "svm"
  }

  // - entry point -----
  def main(args: Array[String]) = {
    val aargs = if( args.size > 0) args else Array("--help")
    val c = new Conf(aargs)
    c.subcommand match {
      case e if e.get == c.export ⇒ export(c)
      case r if r.get == c.train ⇒ train(c)
      case t if t.get == c.test ⇒ test(c)
    }
  }
}
