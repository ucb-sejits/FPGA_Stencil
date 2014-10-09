package Stencil

import Chisel._

//Define a stallable, parameterizable shift register.
object EnableShiftRegister
{
  def apply[T <: Data](in: T, n: Int, en: Bool = Bool(true)): T =
  {
    if (n == 1) {
      RegEnable(in, en)
    } else if (n != 0) {
      RegEnable(apply(in, n-1, en), en)
    } else {
      in
    }
  }
}

//TODO Parameterizable up counters that increment on a signal.

//Define a connection between the delay line with the window arithmatic.
class GridConnection extends Bundle {
  val x_counter = UInt()
  val y_counter = UInt()
  val valid = Bool()
  //TODO implement vec of vecs.
  val row1 = Vec.fill(3){ UInt() }
  val row2 = Vec.fill(3){ UInt() }
  val row3 = Vec.fill(3){ UInt() }
}

class WindowArithmatic extends Module {
  val io = new Bundle {
    val gc = new GridConnection().asInput
    val stream_out = UInt(OUTPUT, 8)
  }
  //TODO look at x and y counters to handle invalid cases
  
  when (io.gc.valid) {
    io.stream_out := (io.gc.row1(0) + io.gc.row1(1) + io.gc.row1(2) + io.gc.row2(0) + io.gc.row2(1) + io.gc.row2(2) + io.gc.row3(0) + io.gc.row3(1) + io.gc.row3(2)) 
  }
  
}

class Stencil extends Module {
  val io = new Bundle { 
    val stream_in = UInt(INPUT, 8)
    val valid = Bool(INPUT)
    val stream_out = UInt(OUTPUT, 8)
  }

  //Initialize discrete registers and wires of delay line.
  val i1 = UInt()
  val i2 = Reg(UInt())
  val i3 = Reg(UInt())
  val i4 = UInt()
  val i5 = Reg(UInt())
  val i6 = Reg(UInt())
  val i7 = UInt()
  val i8 = Reg(UInt())
  val i9 = Reg(UInt())

  //Initialize x and y counters
  val x_counter = Reg(init = UInt(0, 16))
  val y_counter = Reg(init = UInt(0, 16))

  //Update the indexes that are registers.
  when (io.valid) {
    i2 := i1
    i3 := i2
    i5 := i4
    i6 := i5
    i8 := i7
    i9 := i8
  }

  //Update the indexes that are wires.
  i1 := io.stream_in
  i4 := EnableShiftRegister(i3, 98, io.valid)
  i7 := EnableShiftRegister(i6, 98, io.valid)

  //Update counter logic
  when (io.valid) {
    when (x_counter >= UInt(99, 16)) {
      x_counter := UInt(0, 16)
      when (y_counter >= UInt(99, 16)) {
        y_counter := UInt(0, 16)
      } .otherwise {
        y_counter := UInt(1, 16) + y_counter
      }
    } .otherwise {
      x_counter := UInt(1, 16) + x_counter
    }
  }

  //TODO Figure out why using connection results in null pointer connection

  /*
  //Initialize a connection between the delay line with the window arithmatic.
  val gc = new GridConnection().asOutput
  gc.x_counter := x_counter
  gc.y_counter := y_counter
  gc.valid := io.valid
  gc.row1(0) := i9
  gc.row1(1) := i8
  gc.row1(2) := i7
  gc.row2(0) := i6
  gc.row2(1) := i5
  gc.row2(2) := i4
  gc.row3(0) := i3
  gc.row3(1) := i2
  gc.row3(2) := i1

  //Initialize window arithmatic module
  val window = Module(new WindowArithmatic())
  window.io.gc := gc
  io.stream_out := window.io.stream_out
  */

  io.stream_out := (i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9) / UInt(9, 8)

}


class StencilTests(c: Stencil) extends Tester(c) {
  //val stream_in = 42
  //poke(c.io.stream_in, stream_in)
  //step(1)
  //expect(c.io.stream_out, 42)
}

object Stencil {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    chiselMainTest(tutArgs, () => Module(new Stencil())) {
      c => new StencilTests(c) }
  }
}