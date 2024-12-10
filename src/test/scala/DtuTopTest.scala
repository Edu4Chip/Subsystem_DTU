import chisel3._
import chisel3.util.experimental.BoringUtils
import io._
import leros._
import config._

class DtuTopTest(prog:String) extends Module {

  val dtuTop = Module(new DtuTop(progROM = prog, resetSyncFact = () => Module(new ResetSyncTest())))
  // val programmer = Module(new Programmer(dtuTop.lerosClockFreq, dtuTop.lerosUartBaudrate, prog))
  val io = IO( new Bundle{
    val dbg = new Debug(LEROS_CONFIG.ACCU_SIZE, LEROS_CONFIG.DMEM_ADDR_WIDTH)
    val apb = new ApbTargetPort(APB_CONFIG.ADDR_WIDTH, APB_CONFIG.DATA_WIDTH)
    val pmod0 = new PmodGpioPort()
    val pmod1 = new PmodGpioPort()

  })

  dtuTop.reset := ~reset.asBool

  // Boring Utils for debugging
  io.dbg.accu := DontCare
  io.dbg.pc := DontCare
  io.dbg.instr := DontCare
  io.dbg.exit := DontCare
  BoringUtils.bore(dtuTop.lerosController.leros.accu, Seq(io.dbg.accu))
  BoringUtils.bore(dtuTop.lerosController.leros.pcReg, Seq(io.dbg.pc))
  BoringUtils.bore(dtuTop.lerosController.leros.instr, Seq(io.dbg.instr))
  BoringUtils.bore(dtuTop.lerosController.leros.exit, Seq(io.dbg.exit))
  
  io.apb <> dtuTop.io.apb
  io.pmod0 <> dtuTop.io.pmod0
  io.pmod1 <> dtuTop.io.pmod1

  dtuTop.io.irqEn1 := false.B
  dtuTop.io.ssCtrl1 := 0.U
}

