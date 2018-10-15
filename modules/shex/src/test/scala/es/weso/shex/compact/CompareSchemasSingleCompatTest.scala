package es.weso.shex.compact

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import es.weso.json.JsonTest
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shex._
import es.weso.shex.implicits.decoderShEx._
import es.weso.utils.FileUtils._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.{EitherValues, FunSpec, Matchers}
import es.weso.shex.implicits.encoderShEx._

import scala.io._

class CompareSchemasSingleCompatTest extends FunSpec with JsonTest with Matchers with EitherValues {

  val name = "1dotLNex"
  val conf: Config = ConfigFactory.load()
  val schemasFolder = conf.getString("schemasFolder")

  describe(s"Parsing single File $name") {
    val file: File = getFileFromFolderWithExt(schemasFolder, name, "shex")
    it(s"Should read Schema from file ${file.getName}") {
      val str = Source.fromFile(file)("UTF-8").mkString
      Schema.fromString(str, "SHEXC", None,RDFAsJenaModel.empty) match {
        case Right(schema) => {
          val (name, ext) = splitExtension(file.getName)
          val jsonFile = schemasFolder + "/" + name + ".json"
          val jsonStr = Source.fromFile(jsonFile)("UTF-8").mkString
          decode[Schema](jsonStr) match {
            case Left(err) => fail(s"Error parsing $jsonFile: $err")
            case Right(expectedSchema) =>
              if (compareSchemas(schema, expectedSchema)) {
                info("Schemas are equal")
              } else {
                fail(s"Schemas are different. Parsed:\n${schema}\n-----Expected:\n${expectedSchema}\nParsed as Json:\n${schema.asJson.spaces2}\nExpected as Json:\n${expectedSchema.asJson.spaces2}")
              }
          }
        }
        case Left(err) => fail(s"Parsing error: $err")
      }
    }
  }

  // Compare schemas ignoring namespaces or other minor differences like None vs Some(false)
  def compareSchemas(s1: Schema, s2: Schema): Boolean = {
    val s1map = s1.shapesMap
    val s2map = s2.shapesMap
    if (s1map.keys == s2map.keys) {
      s1map == s2map
    } else {
      println(s"Different labels: \n${s1map.keys}\n${s2map.keys}")
      false
    }
  }
}
