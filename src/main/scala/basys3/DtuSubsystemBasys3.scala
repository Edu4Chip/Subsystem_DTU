package basys3

import circt.stage.ChiselStage

import chisel3._
import chisel3.util._
import dtu.DtuSubsystem
import dtu.DtuSubsystemConfig
import io._
import chisel3.experimental.Analog
import mem.MemoryFactory

object DtuSubsystemBasys3 extends App {
  MemoryFactory.use(mem.ChiselSyncMemory.create)
  ChiselStage.emitSystemVerilogFile(
    new DtuSubsystemBasys3(
      DtuSubsystemConfig.default
        .copy(romProgramPath = args.head)
    ),
    "--split-verilog" +: (args.tail),
    Array()
  )
}

class DtuSubsystemBasys3(conf: DtuSubsystemConfig) extends Module {

  val io = IO(new Bundle {
    val pmod = Analog((conf.gpioPins - 4).W)
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

  val pmodDriver = Module(new Tristate(conf.gpioPins - 4))
  pmodDriver.io.busDriveValue := dtu.io.gpio.out
  pmodDriver.io.driveBus := ~dtu.io.gpio.outputEnable
  pmodDriver.io.bus <> io.pmod

  io.leds := dtu.io.gpio.out(math.min(15, conf.gpioPins - 1), 4)

  when(io.ponteEnable) {
    io.uart.tx := dtu.io.gpio.out(0)
    dtu.io.gpio.in := pmodDriver.io.busReadValue ## Cat(
      1.B,
      1.B,
      io.uart.rx,
      1.B
    )
  } otherwise {
    io.uart.tx := dtu.io.gpio.out(2)
    dtu.io.gpio.in := pmodDriver.io.busReadValue ## Cat(
      io.uart.rx,
      1.B,
      1.B,
      1.B
    )
  }

}
