name := "simple-test"

scalaVersion := "2.11.7"

TaskKey[Unit]("check") := {
  val lastLog: File = BuiltinCommands.lastLogFile(state.value).get
  val last: String = IO.read(lastLog)
  assert(last.contains("com.blstream.akkaviz:monitoring"))
}
