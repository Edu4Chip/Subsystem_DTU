package basys3

import chisel3.stage
import dtu.DtuSubsystem
import dtu.DtuSubsystemConfig
import dtu.DtuSerialDriver

class DtuSubsystemBasys3(conf: DtuSubsystemConfig)
    extends Basys3Top(conf, new DtuSubsystem(conf))

object DtuSubsystemBasys3 extends App {
  (new stage.ChiselStage).emitSystemVerilog(
    new DtuSubsystemBasys3(
      DtuSubsystemConfig.default
        .copy(romProgramPath = args.head)
    ),
    args.tail
  )
}

object ProgramDtuSubsystemBasys3 extends App {

  val port = new DtuSerialDriver("/dev/ttyUSB1", 921600)

  port.resetLerosEnable()
  port.selectBootRam()

  Thread.sleep(100)

  port.uploadProgram(args.headOption.getOrElse("leros/asm/didactic_rt.s"))

  port.resetLerosDisable()

  port.close()

}
