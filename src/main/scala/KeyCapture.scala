package com.JeffH001

import com.JeffH001.ANSICodes._
import com.JeffH001.CLITool._
import scala.collection.mutable.LinkedHashMap

/**
  * Main CLITool example code.
  */
object KeyCapture {

	def main (args: Array[String]): Unit = {
		var term = CLITool.init()
		print(s"${ansiBold}Welcome to the CLITool demo!${ansiReset} - Use the ${ansiBkgRGB(51,51,51)} INSERT ${ansiBkgDefault} key to switch modes.")
		CLITool.addMenu(Array("File", "Save", "Load", "Quit"), false)
		CLITool.addMenu(Array("Test", "Force Refresh", "Test Input", "Hide Text", "Test Message", "Long Message", "Very Long Message", "Very Long Message2", "Multi-line Message"))
		var continue = true
		var cmd = new CLITool.Command()
		while (continue) {
			try {
				cmd = CLITool.getCommand()
				print(ansiResetLn)
				cmd.raw match {
					case ""						=>  // Do nothing
					case "q" | "quit" | "Quit"	=> continue = false
					case "Force Refresh"		=> {
						CLITool.termUpdate(true)
						print(ansiResetLn + "Forced screen refresh. ")
					}
					case "Test Input"			=> {
						CLITool.addInput("Input Box", "Message text.", LinkedHashMap(("Username:", "default"), ("~Password:", "")))
					}
					case "Hide Text"			=> {
						CLITool.password = true
						CLITool.updateMenuItem("Hide Text", "Show Text")
					}
					case "Show Text"			=> {
						CLITool.password = false
						CLITool.updateMenuItem("Show Text", "Hide Text")
					}
					case "Test Message"			=> {
						CLITool.addMessage("Message Box", "Message text.", 20)
					}
					case "Long Message"			=> {
						CLITool.addMessage("Message Box", "This is a much longer string of text to make sure that text within a message window wraps properly.", 20)
					}
					case "Very Long Message"	=> {
						CLITool.addMessage("Odd Width Box", "This is a much longer string of text to make sure that text within a message window wraps properly.  Now it should be long enough that the scrollbar will come into effect and we can test whether scrolling works properly.", 21)
					}
					case "Very Long Message2"	=> {
						CLITool.addMessage("Even Width Box", "This is a much longer string of text to make sure that text within a message window wraps properly.  Now it should be long enough that the scrollbar will come into effect and we can test whether scrolling works properly.", 20)
					}
					case "Multi-line Message"	=> {
						CLITool.addMessage("Line Break Test", "This\nis \na\n test \n for\nline breaks.", 20)
					}
					case _ if (cmd.cmdType == "text")	=> {  // Display text input info
						var tmpStr = s"Raw: '${cmd.raw}'\nCommand: '${cmd.command}'"
						var n = 0
						if (cmd.args.length > 0)
							for (a <- cmd.args) {
								tmpStr += s"\nArg: '$a'\nSArg: " + raw"'${cmd.sargs(n)}'"
								n += 1
							}
						CLITool.addMessage("Unknown Command", tmpStr, 100)
					}
					case _ 						=> print(s"Unknown command: '${cmd.raw}'")  // Unknown command
				}
			} catch {
				case e: CLIException => {
					println("\nCLITool error: " + e)
					continue = false
				}
				case e: Exception => {
					println("\nException occurred: " + e)
					continue = false
				}
			}
		}
		CLITool.close()
	}
}