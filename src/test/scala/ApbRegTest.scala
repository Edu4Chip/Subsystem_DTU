import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

class APBTargetTest() extends AnyFlatSpec with ChiselScalatestTester {

    val addrWidth = 32
    val dataWidth = 32
    val baseAddr = 0x00
    val registerCount = 5
    
    val testCases = 100

    "APB Target test" should "pass" in {
        test(new ApbRegTarget(addrWidth, dataWidth, baseAddr, registerCount)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>            

            val registerMapSim = Array.fill(registerCount)(0)

            // at first deassert all signals
            dut.io.apb.paddr.poke(0.U)
            dut.io.apb.pwrite.poke(false.B)
            dut.io.apb.psel.poke(false.B)
            dut.io.apb.penable.poke(false.B)
            dut.io.apb.pwdata.poke(0.U)
            dut.clock.step(2)

            val topAddr = Math.pow(2, registerCount-1).toInt
            for(i <- 0 to testCases) {                                
                val testAddr = Random.nextInt(topAddr + 2).toInt
                val testData = Random.nextInt(0xFFFFFFF).toInt
                
                val action = Random.nextInt(2)
                if(action == 1) {
                    write(testAddr, testData)
                }
                else {
                    read(testAddr)
                }                                
            }

            def write(addr: Int, wdata: Int) = {
                // setup phase        
                dut.io.apb.paddr.poke(addr.U)
                dut.io.apb.pwrite.poke(true.B)
                dut.io.apb.psel.poke(true.B)
                dut.io.apb.penable.poke(false.B)
                dut.io.apb.pwdata.poke(wdata.U)
                dut.clock.step()

                dut.io.apb.pslverr.expect(checkAddrError(addr).B)
                dut.io.apb.pready.expect(true.B)

                // access phase
                dut.io.apb.penable.poke(true.B)
                dut.clock.step()

                // finish
                dut.io.apb.paddr.poke(0.U)
                dut.io.apb.pwrite.poke(false.B)
                dut.io.apb.psel.poke(false.B)
                dut.io.apb.penable.poke(false.B)
                dut.io.apb.pwdata.poke(0.U)
                dut.clock.step()

                // update software simulation
                val idx = ((addr - baseAddr) >> 2) 
                if(idx < registerCount) {
                    registerMapSim(idx) = wdata
                }                
            }

            def read(addr: Int) = {
                // setup phase
                dut.io.apb.paddr.poke(addr.U)
                dut.io.apb.pwrite.poke(false.B)
                dut.io.apb.psel.poke(true.B)
                dut.io.apb.penable.poke(false.B)
                dut.io.apb.pwdata.poke(0.U)
                dut.clock.step()

                dut.io.apb.pslverr.expect(checkAddrError(addr).B)
                dut.io.apb.pready.expect(true.B)
                                
                if(!dut.io.apb.pslverr.peekBoolean()) {
                    val idx = computeIdx(addr)
                    dut.io.apb.prdata.expect(registerMapSim(idx).U) 
                }                                

                // access phase
                dut.io.apb.penable.poke(true.B)
                dut.clock.step()

                // finish + read out read data
                dut.io.apb.paddr.poke(0.U)
                dut.io.apb.pwrite.poke(false.B)
                dut.io.apb.psel.poke(false.B)
                dut.io.apb.penable.poke(false.B)
                dut.io.apb.pwdata.poke(0.U)
                dut.clock.step()                        
            }         
            
            def checkAddrError(addr:Int): Boolean = {
                ((baseAddr > addr) || (((addr - baseAddr) >> 2) > registerCount))
            }

            def computeIdx(addr:Int): Int = {
                val idx = ((addr - baseAddr) >> 2)   
                                       
                val width = Math.ceil((Math.log(registerCount) / Math.log(2))).toInt
                val mask = Math.pow(2, width).toInt - 1;
                val idxBound = idx & mask

                idxBound
            }
        }
    }
}
