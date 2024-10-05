import chisel3._
import chisel3.util._

class ApbTargetPort(addrWidth:Int = 32, dataWidth:Int = 32) extends Bundle() {
    val paddr = Input(UInt(addrWidth.W))
    val psel = Input(Bool())
    val penable = Input(Bool())
    val pwrite = Input(Bool())
    val pwdata = Input(UInt(dataWidth.W))
    val pready = Output(Bool())
    val prdata = Output(UInt(dataWidth.W))
    val pslverr = Output(Bool())
}
