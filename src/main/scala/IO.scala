package ml.tools

import java.io._
import util.Try

// SOme IO validators and helpers
object IO {

  def isValidFile(path: String): Boolean = fileExists(path)
  def fileExists(path: String): Boolean = (new File(path)).isFile

  def isValidBlob(path: String): Boolean = GCS.isValidBlob(path)
  def blobExists(path: String): Boolean = GCS.blobExists(path)

  def isValidSource(path: String): Boolean =
    path match {
      case GCS.GCSPath(_, _) ⇒ isValidBlob(path)
      case _ ⇒ isValidFile(path)
    }
  def sourceExists(path: String): Boolean =
    path match {
      case GCS.GCSPath(_, _) ⇒ blobExists(path)
      case _ ⇒ fileExists(path)
    }

  def isValidTable(path: String): Boolean = BQ.isValidTable(path)
  def tableExists(path: String): Boolean = BQ.tableExists(path)

  def isValidModel(path: String): Boolean = modelExists(path)
  def modelExists(path: String): Boolean = Try(XGB.loadModel(iStream(path))).isSuccess

  def oStream(s: String): OutputStream =
    s match {
      case GCS.GCSPath(b, s) ⇒ GCS.getOutputStream(b, s)
      case _ ⇒ new FileOutputStream(s)
    }
  def iStream(s: String): InputStream =
    s match {
      case GCS.GCSPath(b, s) ⇒ GCS.getInputStream(b, s)
      case _ ⇒ new FileInputStream(s)
    }
}
