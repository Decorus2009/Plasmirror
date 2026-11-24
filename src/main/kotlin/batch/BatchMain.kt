import batch.BatchEnvironment
import batch.USAGE
import batch.compareWithUiExport
import batch.parseArgs
import batch.runBatch
import java.io.File
import kotlin.system.exitProcess

/**
 * Headless entry point of the batch mode (run from the root of the project):
 * `java -cp <jar-with-dependencies> BatchMain <cases-dir> <out-dir>`
 * or `BatchMain --compare-export <result.json> <export.txt>`. Exit codes: 0, 1 (arguments), 2 (expect not met),
 * 3 (time limit), 4 (comparison found differences).
 */
object BatchMain {
  @JvmStatic
  fun main(args: Array<String>) {
    exitProcess(run(args))
  }

  internal fun run(args: Array<String>): Int {
    BatchEnvironment.install()
    if (args.isNotEmpty() && args[0] == "--compare-export") {
      if (args.size != 3) {
        System.err.println("Expected <result.json> and <export.txt>\n$USAGE")
        return 1
      }
      val resultJson = File(args[1])
      val export = File(args[2])
      if (!resultJson.isFile || !export.isFile) {
        System.err.println("File not found: ${if (!resultJson.isFile) resultJson.path else export.path}")
        return 1
      }
      val problems = compareWithUiExport(resultJson, export)
      problems.forEach { System.err.println(it) }
      return if (problems.isEmpty()) 0 else 4
    }
    val options = try {
      parseArgs(args)
    } catch (ex: IllegalArgumentException) {
      System.err.println(ex.message)
      return 1
    }
    return runBatch(options)
  }
}
