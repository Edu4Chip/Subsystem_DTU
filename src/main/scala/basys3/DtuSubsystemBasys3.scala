package basys3

import chisel3._
import chisel3.util._
import dtu.DtuSubsystem
import dtu.DtuSubsystemConfig
import dtu.DtuSerialDriver
import io.PmodPins
import io.UartPins
import chisel3.experimental.Analog

class DtuSubsystemBasys3(conf: DtuSubsystemConfig) extends Module {

  val io = IO(new Bundle {
    val pmod = Analog(4.W)
    val leds = Output(UInt(16.W))
    val uart = new UartPins
    val ponteEnable = Input(Bool())
  })

  val dtu = Module(new DtuSubsystem(conf))

  dtu.io.ssCtrl := 0.U
  dtu.io.irqEn := 0.B
  dtu.io.apb := DontCare
  dtu.io.apb.psel := 0.B
  dtu.io.apb.penable := 0.B

  val pmodDriver = Module(new Tristate(4))
  dtu.io.pmod(1).gpi := pmodDriver.io.busReadValue
  pmodDriver.io.driveBus := ~dtu.io.pmod(1).oe
  pmodDriver.io.busDriveValue := dtu.io.pmod(1).gpo
  pmodDriver.io.bus <> io.pmod

  io.leds := Cat(dtu.io.pmod(1).gpo, io.uart.rx, dtu.io.pmod(0).gpo(2), io.uart.rx, dtu.io.pmod(0).gpo(0))

  dtu.io.pmod(0).gpi := 0xf.U

  when(io.ponteEnable) {
    io.uart.tx := dtu.io.pmod(0).gpo(0)
    dtu.io.pmod(0).gpi := io.uart.rx ## 0.B
  } otherwise {
    io.uart.tx := dtu.io.pmod(0).gpo(2)
    dtu.io.pmod(0).gpi := io.uart.rx ## Fill(3, 0.B)
  }

}

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

  port.uploadProgram(args.headOption.getOrElse("leros-asm/didactic_rt.s"))

  port.resetLerosDisable()

  port.close()

}

object ReadCCRDtuSubsystemBasys3 extends App {

  val port = new DtuSerialDriver("/dev/ttyUSB1", 921600)

  try{
    while(true) {
    print("\r" + port.readCrossCoreReg(0))
    Thread.sleep(100)
    }
  } catch {
    case e: InterruptedException => println("\nInterrupted")
  } finally {
    port.close()
  }

}