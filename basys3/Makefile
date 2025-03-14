

gen:
	cd .. && sbt "runMain Basys3Top"

synth:
	./vivado_synth -p xc7a35tcpg236-1 -x pins.xdc -t Basys3Top -o ../build ../generated/Basys3Top.sv

prog:
	openocd -f basys3_prog.f