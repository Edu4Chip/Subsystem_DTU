use clap::{Parser, Subcommand};
use serialport::SerialPort;
use std::io::{self, Read, Write};
use std::time::Duration;

/// Simple CLI for communicating with the Ponte Serial → APB bridge
#[derive(Parser)]
#[command(author, version, about)]
struct Args {
    /// Serial port device (e.g. /dev/ttyUSB0 or COM3)
    #[arg(short, long)]
    port: String,

    /// Baud rate (default: 115200)
    #[arg(short, long, default_value_t = 115200)]
    baud: u32,

    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Read a 32-bit word from address
    Read {
        /// Number of 32-bit words to read
        #[arg(short='n',default_value_t = 1)]
        words: usize,
        /// Byte address to read from   
        #[arg(short, long, value_parser = parse_hex)]
        address: u32,
        
    },
    /// Write a 32-bit word to address
    Write {
      /// Start byte address to write to
      #[arg(short, long, value_parser = parse_hex)]
      address: u32,
      /// One or more 32-bit values to write (hex like 0x1234 or decimal)
      #[arg(value_parser = parse_hex, num_args = 1..)]
      values: Vec<u32>,
    },

    WriteFile {
        /// Byte address to dump the file contents to
        #[arg(short, long, value_parser = parse_hex)]
        address: u32,
        /// Path to the file to write
        #[arg()]
        file_path: String,
    },

    ReadFile {
        /// Byte address to read from
        #[arg(short, long, value_parser = parse_hex)]
        address: u32,
        /// Number of 32-bit words to read
        #[arg(short='n')]
        words: usize,
        /// Path to the file to save the data to
        #[arg()]
        file_path: String,
    },
}

fn parse_hex(s: &str) -> Result<u32, std::num::ParseIntError> {
    if let Some(stripped) = s.strip_prefix("0x") {
        u32::from_str_radix(stripped, 16)
    } else {
        s.parse()
    }
}

fn main() -> io::Result<()> {
    let args = Args::parse();

    let mut port = serialport::new(&args.port, args.baud)
        .timeout(Duration::from_millis(1000))
        .open()?;

    match args.command {
        Command::Read { address, words } => {
            let response = read(&mut port, address, words)?;
            for word in response {
                print!("0x{:08X} ", word);
            }
            println!();
        }
        Command::Write { address, values } => {
            send(&mut port, address, &values)?;
        }
        Command::WriteFile { address, file_path } => {
            let mut file = std::fs::File::open(file_path)?;
            let mut buffer = Vec::new();
            file.read_to_end(&mut buffer)?;

            let mut data_words = Vec::new();
            for chunk in buffer.chunks(4) {
                let mut word: u32 = 0;
                for (i, &byte) in chunk.iter().enumerate() {
                    word |= (byte as u32) << (i * 8);
                }
                data_words.push(word);
            }

            send(&mut port, address, &data_words)?;
        }
        Command::ReadFile { address, words: length, file_path } => {
            let data_words = read(&mut port, address, length)?;

            let mut file = std::fs::File::create(file_path)?;
            for &word in &data_words {
                let bytes = [
                    (word & 0xFF) as u8,
                    ((word >> 8) & 0xFF) as u8,
                    ((word >> 16) & 0xFF) as u8,
                    ((word >> 24) & 0xFF) as u8,
                ];
                file.write_all(&bytes)?;
            }
        }
    }

    Ok(())
}

// === Protocol handling ===

const START_WR: u8 = 0xaa;
const START_RD: u8 = 0xab;
const ESC: u8 = 0x5a;
const ESC_MASK: u8 = 0x20;

fn add_bytes(buf: &mut Vec<u8>, value: u32, n: usize) {
  for i in 0..n {
      let byte = ((value >> (i * 8)) & 0xFF) as u8;
      if byte == START_WR || byte == START_RD || byte == ESC {
          buf.push(ESC);
          buf.push(byte ^ ESC_MASK);
      } else {
          buf.push(byte);
      }
  }
}

fn send(port: &mut Box<dyn SerialPort>, addr: u32, data: &Vec<u32>) -> io::Result<()> {
  let mut frame = Vec::new();

  // Start byte
  frame.push(START_WR);

  // Address (2 bytes, little endian + escaping)
  add_bytes(&mut frame, addr, 2);

  // Data words (4 bytes each, little endian + escaping)
  for &word in data {
      add_bytes(&mut frame, word, 4);
  }

  // Send the frame
  port.write_all(&frame)?;
  port.flush()?;

  Ok(())   

}



fn read(port: &mut Box<dyn SerialPort>, addr: u32, n: usize) -> io::Result<Vec<u32>> {
  let mut frame = Vec::new();
  
  frame.push(START_RD);
  frame.push((n - 1) as u8);
  add_bytes(&mut frame, addr, 2);
  port.write_all(&frame)?;
  port.flush()?;


  let mut buf = Vec::new();

  for _ in 0 .. n {

    // read one word (4 bytes)
    let mut word_buf = [0u8; 4];
    port.read_exact(&mut word_buf)?;
    buf.push(((word_buf[3] as u32) << 24) | ((word_buf[2] as u32) << 16) | ((word_buf[1] as u32) << 8) | word_buf[0] as u32);
  }

  Ok(buf)

  

}