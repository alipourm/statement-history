package edu.oregonstate.mutation.statementHistory

import java.io.{FileOutputStream, PrintStream, OutputStream, File}
import java.util.logging.Level

import fr.labri.gumtree.matchers.Matcher

object Main {

  private case class Config(method: Boolean = false,
                        repo: File = new File("."),
                        jsonFile: File = new File("."),
                        commit: String = "HEAD",
                        file:Option[String] = None) {
  }

  private def parseCmdOptions(args: Array[String]): Option[Config] = {
    val parser = new scopt.OptionParser[Config]("java -jar <jar_name>") {
      opt[Boolean]('m', "method") action { (x,c) =>
          c.copy(method = x)
        } text ("track methods; default is false, it tracks statements")
      opt[String]('r', "repo") required() action { (x,c) =>
        c.copy(repo=new File(x))
      } text("The location of the repository")
      opt[String]('j', "json-file") required() action { (x,c) =>
        c.copy(jsonFile = new File(x))
      } text("The json file with the mutants")
      opt[String]('c', "commit") action{ (x,c) =>
        c.copy(commit = x)
      } text("The commit to reference the line number to; default is HEAD")
      opt[String]('f', "output-file") action { (x,c) =>
        c.copy(file = Some(x))
      } text("The output file")
    }

    parser.parse(args, Config())
  }

  private def doAnalysis(config: Config): Unit = {
    disableLoggers()

    val finder = if (config.method)
      MethodFinder
    else
      StatementFinder
    val detector = new NodeChangeDetector(config.repo, finder)
    val mutants = JSONDecoder.decode(config.jsonFile)

    val outputStream = config.file match {
      case Some(x) => new PrintStream(new FileOutputStream(new File(x)))
      case None => System.out
    }

    val result = mutants.toParArray.map(mutant => {
      mutant.getFileName + "," + mutant.getLineNumber + "," +
        detector.findCommits(mutant.getFileName, mutant.getLineNumber).map(commit => commit + ",").
          foldRight[String]("")((c, e) => c + e) + "\n"
    }).asParSeq.reduceRight((current, element) => current + element)

    outputStream.write(result.getBytes)
  }

  private def disableLoggers() = {
    Matcher.LOGGER.setLevel(Level.OFF)
  }

  def main(args: Array[String]) = {

    val config = parseCmdOptions(args)
    config match {
      case Some(x) =>
        doAnalysis(x)
      case None => println("Incorrect args")
    }

  }
}