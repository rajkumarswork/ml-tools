package ml.tools

import com.google.cloud.storage._
import java.nio.channels.Channels
import java.io._

// wrappers to help read/write to Google Cloud Storage
object GCS {

  // Writers
  def getOutputStream(bucket: String, prefix: String): OutputStream = {
    val blobid = BlobId.of(bucket, prefix)
    val blobinfo = BlobInfo.newBuilder(blobid).setContentType("application/octet-stream").build();
    return Channels.newOutputStream(storage.writer(blobinfo));
  }

  // Readers
  def getInputStream(bucket: String, prefix: String): InputStream =
    blobStream(storage.get(bucket, prefix))

  // Helpers
  def pathInputStream(path: String): InputStream = {
    val (bucket, prefix) = GCS.parseGCSPath(path)
    if (bucket.size == 0) new FileInputStream(prefix)
    else getInputStream(bucket, prefix)
  }
  def pathOutputStream(path: String): OutputStream = {
    val (bucket, prefix) = GCS.parseGCSPath(path)
    if (bucket.size == 0) new FileOutputStream(prefix)
    else getOutputStream(bucket, prefix)
  }

  // - privates --
  private val storage = StorageOptions.newBuilder().build().getService()

  private def blobStream(b: Blob): InputStream = Channels.newInputStream(b.reader())

  val GCSPath = """^gs://([\w_-]+)/(.+$)""".r
  private def parseGCSPath(path: String): (String, String) =
    path match {
      case GCSPath(bucket, prefix) ⇒ (bucket, prefix)
      case _ ⇒ ("", path)
    }
  def blobId(path: String): BlobId = {
    val (b, p) = parseGCSPath(path)
    BlobId.of(b, p)
  }
  def isValidBlob(path: String): Boolean = parseGCSPath(path)._1.size > 0
  def blobExists(path: String): Boolean = storage.get(blobId(path)) != null
}
