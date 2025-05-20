package basys3

import dtu.DtuSerialDriver
import dtu.DtuSubsystemConfig

object ProgramDtuSubsystemBasys3 extends App {

  val dev = args.headOption.getOrElse("/dev/ttyUSB0")
  val prog = args.tail.headOption.getOrElse("leros-asm/didactic.s")

  val port = new DtuSerialDriver(dev, DtuSubsystemConfig.default.ponteBaudRate)

  port.resetLerosEnable()
  port.selectBootRam()

  Thread.sleep(100)

  port.uploadProgram(prog)

  port.resetLerosDisable()

  port.close()

}

object ReadCCRDtuSubsystemBasys3 extends App {

  val dev = args.headOption.getOrElse("/dev/ttyUSB0")

  val port = new DtuSerialDriver(dev, DtuSubsystemConfig.default.ponteBaudRate)

  try {
    while (true) {
      print("\r" + port.readCrossCoreReg(0))
      Thread.sleep(100)
    }
  } catch {
    case e: InterruptedException => println("\nInterrupted")
  } finally {
    port.close()
  }

}
