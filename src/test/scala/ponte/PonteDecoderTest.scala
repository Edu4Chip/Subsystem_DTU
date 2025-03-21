package ponte

import chisel3._

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random

import ponte._
import apb.ApbTargetBfm
import apb.ApbTargetBfm._
import misc.FormalHelper

class PonteDecoderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "PonteDecoder"

  FormalHelper.withPropertiesEnabled {

    it should "decode a frame" in {
      test(new PonteDecoder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val ponte = new PonteDecoderBfm(dut)
        val apb = new ApbTargetBfm(dut.io.apb, dut.clock)

        Random.setSeed(0x34df3)
        val n = 256
        val testVec = Seq.fill(n)(BigInt(32, Random))
        val accesses = Random.shuffle(Seq.range(0, n))

        fork {
          for (_ <- 0 until (2 * n)) {
            apb.next() match {
              case Read(addr) =>
                apb.respondRead(testVec(addr.toInt))
              case Write(addr, data) =>
                assert(data == testVec(addr.toInt))
                apb.respondWrite()
              case _ =>
                fail("Invalid operation")
            }
          }
        }

        for (addr <- accesses) {
          ponte.write(addr, Seq(testVec(addr)))
        }

        for (addr <- accesses) {
          val read = ponte.read(addr, 1).head
          assert(read == testVec(addr))
        }
      }
    }
  }
}
