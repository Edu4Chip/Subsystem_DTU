import chisel3._
import chisel3.util._

class APBTargetPort(addrWidth:Int = 32, dataWidth:Int = 32) extends Bundle() {
    val paddr = Input(UInt(addrWidth.W))
    val psel = Input(Bool())
    val penable = Input(Bool())
    val pwrite = Input(Bool())
    val pwdata = Input(UInt(dataWidth.W))
    val pready = Output(Bool())
    val prdata = Output(UInt(dataWidth.W))
    val pslverr = Output(Bool())
}

class APBTarget(addrWidth:Int = 32, dataWidth:Int = 32, baseAddr:Int = 0, registerCount:Int = 5) extends Module {
    val io = IO(new Bundle{
        val apb = new APBTargetPort(addrWidth, dataWidth)
    })

    val idle :: write :: read :: Nil = Enum(3)
    val state = RegInit(idle)

    val registerMap = RegInit(VecInit(Seq.fill(5)(0.U(dataWidth.W))))
    val registerIndex = WireDefault(0.U(log2Ceil(registerCount).W))
    registerIndex := (io.apb.paddr - baseAddr.U) >> 2

    io.apb.pslverr := (baseAddr.U > io.apb.paddr) || (((io.apb.paddr - baseAddr.U) >> 2) > (registerCount-1).U)
    io.apb.pready := false.B
    io.apb.prdata := 0.U

    switch(state) {
        is(idle) {
            io.apb.pready := false.B
            io.apb.prdata := 0.U

            when(io.apb.psel && io.apb.pwrite) {
                state := write
            }
            . elsewhen(io.apb.psel && !io.apb.pwrite) {
                state := read
            }
        }
        is(write) {
            registerMap(registerIndex) := io.apb.pwdata
            io.apb.pready := true.B
            
            state := idle
        }
        is(read) {
            io.apb.prdata := registerMap(registerIndex)
            io.apb.pready := true.B

            state := idle        
        }
    }

}

object APBTarget extends App {
    emitVerilog(new APBTarget(), Array("--target-dir", "generated"))
}