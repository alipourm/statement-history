package edu.oregonstate.mutation.statementHistory

import java.io.File

import org.eclipse.jdt.core.dom.ASTNode
import play.api.libs.json.Json

import scala.io.Source

object JSONDecoder extends Decoder {

  def decodeLine(line: String): StatementInfo = {
    val parsed = Json.parse(line)
    val file = (parsed \ "mutant" \ "filename").as[String]
    val rawLineNo = parsed \ "mutant" \ "line"
    val lineNo = rawLineNo.asOpt[Int] match {
      case Some(x) => x
      case _ => rawLineNo.as[String].toInt
    }
    val className = (parsed \ "mutant" \ "id" \ "location" \ "class").as[String]
    return new StatementInfo(file, lineNo, className)
  }

  def decode(file: File, find: (String, Int) => ASTNode): Seq[StatementInfo] = {
    val statements = Source.fromFile(file).getLines().map(decodeLine).toSeq
    computeExtraInfo(find, statements)
  }

  def decode(json: String, find: (String, Int) => ASTNode) : Seq[StatementInfo] = {
    val statements = decode(json)
    computeExtraInfo(find, statements)
  }

  private def computeExtraInfo(find: (String, Int) => ASTNode, statements: Seq[StatementInfo]): Seq[StatementInfo] = {
    statements.foreach(s => {
      val node = find(s.getFileName, s.getLineNumber)
      s.computeOtherInfo(node)
    })
    return statements
  }
}
