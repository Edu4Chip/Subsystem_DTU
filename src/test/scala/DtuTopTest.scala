import didactic.PmodGpioPort
import apb.ApbTargetPort
import chisel3._
import chisel3.util.experimental.BoringUtils
import leros._
import config._
import dtu.DtuSubsystem
import dtu.peripherals._

class DtuTestHarness(prog: String) extends Module {

  val io = IO(new Bundle {
    val dbg = new Debug(32, 16)
    val apb = new ApbTargetPort(12, 32)
    val uart = new UartPins
    val bootSel = Input(Bool())
    val pmod1 = new PmodGpioPort
  })

  val dtu = Module(new DtuSubsystem(prog))

  dtu.reset := ~reset.asBool

  // Boring Utils for debugging
  io.dbg.accu := DontCare
  io.dbg.pc := DontCare
  io.dbg.instr := DontCare
  io.dbg.exit := DontCare
  BoringUtils.bore(dtu.leros.accu, Seq(io.dbg.accu))
  BoringUtils.bore(dtu.leros.pcReg, Seq(io.dbg.pc))
  BoringUtils.bore(dtu.leros.instr, Seq(io.dbg.instr))
  BoringUtils.bore(dtu.leros.exit, Seq(io.dbg.exit))

  io.apb <> dtu.io.apb
  io.pmod1 <> dtu.io.pmod(1)
  dtu.io.pmod(0).gpi(0) := io.bootSel
  dtu.io.pmod(0).gpi(1) := io.uart.rx
  io.uart.tx := dtu.io.pmod(0).gpo(2)

  dtu.io.irqEn := false.B
  dtu.io.ssCtrl := 0.U
}
