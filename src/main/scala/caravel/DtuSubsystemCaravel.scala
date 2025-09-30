package caravel

import circt.stage.ChiselStage

import chisel3._
import chisel3.util._
import dtu.DtuSubsystem
import dtu.DtuSubsystemConfig
import io._
import chisel3.experimental.Analog
import mem.MemoryFactory

object DtuSubsystemCaravel extends App {
  MemoryFactory.use(mem.ChiselSyncMemory.create)
  ChiselStage.emitSystemVerilogFile(
    new DtuSubsystemCaravel(
      DtuSubsystemConfig.default
        .copy(romProgramPath = args.head)
    ),
    // "--split-verilog" +: (args.tail),
    (args.tail),
    Array()
  )
}

class DtuSubsystemCaravel(conf: DtuSubsystemConfig) extends Module {

/* 
    // Wishbone Slave ports (WB MI A)
    input wb_clk_i,
    input wb_rst_i,
    input wbs_stb_i,
    input wbs_cyc_i,
    input wbs_we_i,
    input [3:0] wbs_sel_i,
    input [31:0] wbs_dat_i,
    input [31:0] wbs_adr_i,
    output wbs_ack_o,
    output [31:0] wbs_dat_o,

    // Logic Analyzer Signals
    input  [127:0] la_data_in,
    output [127:0] la_data_out,
    input  [127:0] la_oenb,

    // IOs
    input  [BITS-1:0] io_in,
    output [BITS-1:0] io_out,
    output [BITS-1:0] io_oeb,

    // IRQ
    output [2:0] irq
     */
  val io = IO(new Bundle {
    // val wb_clk_i = Input(Clock())
    // val wb_rst_i = Input(Bool())
    val wbs_stb_i = Input(Bool())
    val wbs_cyc_i = Input(Bool())
    val wbs_we_i = Input(Bool())
    val wbs_sel_i = Input(UInt(4.W))
    val wbs_dat_i = Input(UInt(32.W))
    val wbs_adr_i = Input(UInt(32.W))
    val wbs_ack_o = Output(Bool())
    val wbs_dat_o = Output(UInt(32.W))

    val la_data_in = Input(UInt(128.W))
    val la_data_out = Output(UInt(128.W))
    val la_oenb = Input(UInt(128.W))

    // the wrapper uses only 16 bits, maybe OK for now
    val io_in = Input(UInt(16.W))
    val io_out = Output(UInt(16.W))
    val io_oeb = Output(UInt(16.W))

    val irq = Output(UInt(3.W))
  })

  io.wbs_ack_o := false.B
  io.wbs_dat_o := 0.U
  io.la_data_out := 0.U
  io.io_out := 0.U
  io.io_oeb := 0.U
  io.irq := 0.U
  // val dtu = Module(new DtuSubsystem(conf))

/*
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
*/
}
