package com.JeffH001

/**
  * CLITool: A tool to simplify creating and working with a Command Line Interface (CLI).  (v0.1 - by JeffH001)
  * - Note: build.sbt must contain `libraryDependencies += "org.jline" % "jline" % "3.21.0"` or other compatible version.
  */
package object CLITool {
	import scala.collection.mutable.LinkedHashMap
	import scala.collection.mutable.ArrayBuffer
	import org.jline.terminal.TerminalBuilder
	import org.jline.utils.NonBlockingReader
	import java.util.Calendar
	import com.JeffH001.ANSICodes._

	// Custom classes:

	/**
	  * The object returned from a .getCommand() method call.  All properties default to empty versions of their values.
	  *
	  * @param cmdType	The command type.  Possible types: `text`, `menu`, `window`, `error`, default = `empty`
	  * @param raw		The full command string or error string.
	  * @param command	The selected menu or list item, first word of a command string, or `"Error"` if an error occurs.
	  * @param args		A list of inputs or the remaining words in the command string.  Text type args can be grouped using double-quotes (e.g. all the text within `"This is a test."` would be one arg element).
	  * @param sargs	A "sanitized" version of the `args` property, with any backslashes or apostrophes escaped.
	  * @param index	An index value; used by some windows.  (default = `-1`)
	  */
	class Command (
		var cmdType: String = "empty",
		var raw: String = "",
		var command: String = "",
		var args: List[String] = List.empty[String],
		var sargs: List[String] = List.empty[String],
		var index: Int = -1
	)

	/**
	  * CLI "message" window object, displays a message within a window.
	  *
	  * @param label			Label on the window.
	  * @param message			Message to display in the window.
	  * @param maxWidth			Maximum width of a line of text within the message window.  `-1` = max width (default)
	  * @param locked			Whether the focus is locked on this window or not.  (default = `true`)
	  * @param numLines			Number of lines of text within the window.  (default = `-1`)
	  * @param maxVisibleLines	Maximum number of lines within the window at one time.  (default = `-1`)
	  * @param scrollPos		Scroll position within the window.  (default = `0`)
	  * @param maxScroll		Maximum scroll position within the window.  (default = `0`)
	  */
	class CLIMsgWin (
		var label: String,
		var message: String,
		var maxWidth: Int = -1,
		var locked: Boolean = true,
		var numLines: Int = -1,
		var maxVisibleLines: Int = -1,
		var scrollPos: Int = 0,
		var maxScroll: Int = 0
	)

	/**
	  * CLI "input" window object, used for generating windows which accept user text input.
	  *
	  * @param label		Label on the window.
	  * @param message		Message to display at the top of the input window.  (default = "")
	  * @param items		A LinkedHashMap of Label -> Value entries for each of the input lines.
	  * @param cursorPos	Position of the cursor in each input line.
	  * @param displayPos	Left-most character of the input text that's visible in each input line.
	  * @param width		Width of the content within the window.
	  * @param index		Current input line, -2 for "OK", or -1 for "Cancel".  (default = `0`)
	  * @param scrollPos	Scroll position within the window.  (default = `0`)
	  * @param maxScroll	Maximum scroll position within the window.  (default = `0`)
	  * @param inputX		X position on the screen for the start of the input line.
	  * @param inputX		Y position on the screen for the start of the input line.
	  */
	class CLIInputWin (
		var label: String,
		var message: String = "",
		var items: LinkedHashMap[String, String],
		var cursorPos: Array[Int],
		var displayPos: Array[Int],
		var width: Int,
		var index: Int = 0,
		var scrollPos: Int = 0,
		var maxScroll: Int = 0,
		var inputX: Int = 0,
		var inputY: Int = 0
	)

	/**
	  * CLI "list" window object, used for generating windows with selectable lists of items.
	  *
	  * @param label		Label on the window.
	  * @param items		Items listed in the window.
	  * @param index		The currently selected index from the items list.  `-1` = nothing selected.  (default = `-1`)
	  * @param x			Left position of the upper-left corner of the window.  (minimum of 1)
	  * @param y			Top position of the upper-left corner of the window.  (minimum of 1)
	  * @param width		Width of the content within the window.  (minimum of 1)
	  * @param height		Height of the content of within the window.  (minimum of 1)
	  * @param unselected	Selected item formatting string.  (default = `ansiBkgCyan + ansiTxtBlack`)
	  * @param selected		Unselected item formatting string.  (default = `ansiBkgCyanL + ansiTxtBlack`)
	  * @param borderStyle	Window border formatting string.  (default = `ansiBkgDefault + ansiTxtWhite`)
	  * @param labelStyle	Window label formatting string for when window is not active.  (default = `ansiBkgDefault + ansiTxtWhite`)
	  * @param activeStyle	Window label formatting string for when window is active.  (default = `ansiBkgWhite + ansiTxtBlack`)
	  */
	class CLIListWin (
		var label: String,
		var items: List[String],
		var index: Int = -1,
		var x: Int,
		var y: Int,
		var width: Int,
		var height: Int,
		var unselected: String = ansiBkgCyan + ansiTxtBlack,
		var selected: String = ansiBkgCyanL + ansiTxtBlack,
		var borderStyle: String = ansiBkgDefault + ansiTxtWhite,
		var labelStyle: String = ansiBkgDefault + ansiTxtWhite,
		var activeStyle: String = ansiBkgWhite + ansiTxtBlack
	)

	/**
	  * For generic CLI window object use.
	  *
	  * @param objType	The CLI window object type.  ("message", "list", "input")
	  * @param obj		The CLI window object.
	  */
	private class CLIWinObj (var objType: String, var obj: Any) {
		def label (): String = {
			objType match {
				case "message" => obj.asInstanceOf[CLIMsgWin].label
				case "input" => obj.asInstanceOf[CLIInputWin].label
				case "list" => obj.asInstanceOf[CLIListWin].label
				case _ => "" // Unknown CLI window object type.  (shouldn't happen)
			}
		}
		def message (): String = {
			objType match {
				case "message" => obj.asInstanceOf[CLIMsgWin].message
				case "input" => obj.asInstanceOf[CLIInputWin].message
				case _ => "" // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def locked (): Boolean = {
			objType match {
				case "message" => obj.asInstanceOf[CLIMsgWin].locked
				case _ => false // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def numLines (): Int = {
			objType match {
				case "message" => obj.asInstanceOf[CLIMsgWin].numLines
				case _ => -1 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def scrollPos (): Int = {
			objType match {
				case "message" => obj.asInstanceOf[CLIMsgWin].scrollPos
				case _ => 0 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def index (): Int = {
			objType match {
				case "input" => obj.asInstanceOf[CLIInputWin].index
				case _ => 0 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def x (): Int = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].x
				case _ => -1 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def y (): Int = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].y
				case _ => -1 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def width (): Int = {
			objType match {
				case "input" => obj.asInstanceOf[CLIInputWin].width
				case "list" => obj.asInstanceOf[CLIListWin].width
				case _ => -1 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def height (): Int = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].height
				case _ => -1 // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def borderStyle (): String = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].borderStyle
				case _ => "" // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def labelStyle (): String = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].labelStyle
				case _ => "" // Unknown CLI window object type or unsupported property for the current type.
			}
		}
		def activeStyle (): String = {
			objType match {
				case "list" => obj.asInstanceOf[CLIListWin].activeStyle
				case _ => "" // Unknown CLI window object type or unsupported property for the current type.
			}
		}
	}


	// Variables:

	// Terminal variables
	private var terminal: org.jline.terminal.Terminal = null
	private var reader: NonBlockingReader = null
	private var instance = 0
	private var termWidth = 0
	private var termHeight = 0
	private var maxHeight = 99  // Limit the height of the "windows" area  // *** Add get and set methods for this
	/**
	  * Get current CLITool mode.
	  *
	  * @return	Current CLITool mode ("text", "window", or "menu").
	  */
	def getMode (): String = mode  // Read-only mode access
	/**
	  * Possible modes = "text", "window", or "menu"
	  */
	private var mode = "text"

	// Window mode variables
	private var window = ArrayBuffer[CLIWinObj]()
	private var activewin = -1  // The currently active window when in "window" mode; -1 = none
	private var backgroundColor = ansiBkgRGB("#000033")
	/**
	  * Sets the default text color used in all window elements.  (default = `"#AAAAAA"` = light gray)
	  *
	  * @param rgbString	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  */
	def setWinTxtColor (rgbString: String = "#AAAAAA"): Unit = {
		if ((rgbString.length == 7) && (rgbString.substring(0, 1) == "#")) {
			try {
				winTxtColor = ansiTxtRGB(rgbString)
			} catch {
				case e: Exception => throw new CLIException(s"CLITool setWinTxtColor error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
			}
		} else
			throw new CLIException(s"CLITool setWinTxtColor error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
	}
	/**
	  * Gets the default text color used in all window elements.
	  *
	  * @return	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  */
	def getWinTxtColor (): String = winTxtColor  // Read-only access
	private var winTxtColor = ansiTxtRGB("#AAAAAA")  // Light gray
	/**
	  * Sets the default background color used in all window elements.  (default = `"#000061"` = dark blue)
	  *
	  * @param rgbString	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  */
	def setWinBKGColor (rgbString: String = "#000061"): Unit = {
		if ((rgbString.length == 7) && (rgbString.substring(0, 1) == "#")) {
			try {
				winBkgColor = ansiBkgRGB(rgbString)
			} catch {
				case e: Exception => throw new CLIException(s"CLITool setWinBKGColor error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
			}
		} else
			throw new CLIException(s"CLITool setWinBKGColor error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
	}
	/**
	  * Gets the default background color used in all window elements.
	  *
	  * @return	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  */
	def getWinBkgColor (): String = winBkgColor  // Read-only access
	private var winBkgColor = ansiBkgRGB("#000061")  // Dark blue

	// Text mode variables
	private var inputStr = ""
	private var inputPos = 0
	private var history = ArrayBuffer[String]()
	private var historypos = 0
	private var historymax = 20
	private var prompt = "> "
	private var home = ansiStrip(prompt).length + 1
	/**
	  * Determines if text input is hidden from the screen and the command history.
	  */
	var password = false
	/**
	  * Gets the current text prompt.
	  *
	  * @return Text prompt string.
	  */
	def getPrompt ():String = prompt
	/**
	  * Sets the current text prompt.  (default = "> ")
	  *
	  * @param textPrompt The string to display at the start of the text input line.
	  */
	def setPrompt (textPrompt: String = "> "): Unit = {
		prompt = textPrompt
		home = ansiStrip(prompt).length + 1
	}

	// Menu mode variables
	private var menu = ArrayBuffer[Array[String]]()
	private var menuCol = -1
	private var menuRow = -1
	private var maxRows = 0
	private var menuColor = ansiTxtRGB("#DBD88F") + ansiBkgRGB("#222222")  // Light yellow text (#DBD88F) on a near-black (#222222) background


	// Methods:

	// ======= Shared Methods =======

	/**
	  * A simple SQL string sanitizer.
	  *
	  * @param input	String to be sanitized.
	  * @return			Sanitized string
	  */
	private def sanitizeString (input: String): String = {
		var output = ""
		for(c <- input) {
			c.toInt match {
				case 0	=> output += "\\0"  // NULL
				case 8	=> output += "\\b"  // BACKSPACE
				case 9	=> output += "\\t"  // TAB
				case 10	=> output += "\\n"  // LINEFEED
				case 13	=> output += "\\r"  // CARRIAGE RETURN
				case 26	=> output += "\\Z"  // CTRL+Z / EOF marker
				case 34	=> output += "\\\"" // "
				case 37	=> output += "\\%"  // %
				case 39	=> output += "\\'"  // '
				case 92	=> output += "\\\\" // \
				case 94	=> output += "\\_"  // _
				case _	=> output += c
			}
		}
		output
	}

	/**
	  * Return a string with the character at "pos" underlined to represent the cursor position.
	  * No "cursor" is shown if pos < 0.
	  *
	  * @param txt  String to display a cursor for.
	  * @param cpos Cursor position.
	  * @return     Modified string.
	  */
	private def curstring (txt: String, cpos: Int): String = {
		val tlen = txt.length
		if (cpos < -0)  // No cursor
			txt
		else if (cpos >= tlen)  // Cursor is at the end of the string
			txt + ansiMarker
		else {  // Cursor on the "cpos" position
			var result = ""
			if (cpos > 0)
				result = txt.substring(0, cpos)
			result += ansiUnderline + txt.substring(cpos, cpos + 1) + ansiReset
			if (cpos < tlen - 1)
				result += txt.substring(cpos + 1, tlen)
			result
		}
	}

	/**
	  * Add character to a string at a given position.
	  *
	  * @param inChr	Character to add to the string.  (input.toChar)
	  * @param inStr	String to add it to.  (inputStr)
	  * @param inPos	Positions to add it at.  (inputPos)
	  * @return			Resulting string.
	  */
	private def inputAdd(inChr: Char, inStr: String, inPos: Int): String = {
		var outStr = ""
		if (inPos == inStr.length())  // Append input
			outStr = inStr + inChr
		else {  // Insert input
			if (inPos > 0)
				outStr = inStr.substring(0, inPos)
			outStr += inChr
			if (inPos < inStr.length)
				outStr += inStr.substring(inPos, inStr.length)
		}
		outStr
	}


	// ======= Window Methods =======

	/* Window types:
		message	- A message to display.  Hitting any key closes it.
		input	- A list of inputs for the user to fill in.
		list	- A selectable list of elements.
	*/

	// ------- Message Window Methods -------

	def addMessage (label: String, message: String, maxWidth: Int = -1, locked: Boolean = true): Unit = {
		mode = "window"
		window :+= new CLIWinObj ("message", new CLIMsgWin(label, message, maxWidth, locked))
		activewin = window.length - 1
		drawWindows()
	}


	// ------- Input Window Methods -------

		/*  ┌─ Window Label ─────┐
			│                    │
			│ Message text       │
			│                    │
			│ Input Label 1:     │
			│  ________________  │
			│                    │
			│ Input Label 2:     │
			│  ________________  │
			│                    │
			└───── OK ── Cancel ─┘
		*/

	/**
	  * Add an "input" window.
	  *
	  * @param label	The window label.
	  * @param message	Message to display at the top inside the window.
	  * @param inputs	A list of input labels and their default values.  Labels which start with "~" will have obscured input text.
	  * @param width	Width of the inside of the window.  `-1` = max width (default = `30`)
	  */
	def addInput (label: String, message: String = "", inputs: LinkedHashMap[String, String], width: Int = 30): Unit = {
		var cursorPos = Array.fill[Int](inputs.size)(0)
		var displayPos = Array.fill[Int](inputs.size)(0)
		var idx = -1
		for (i <- inputs) {
			idx += 1
			cursorPos(idx) = i._2.length()
		}
		mode = "window"
		var index = 0
		if (inputs.size == 0)
			index = -2
		window :+= new CLIWinObj ("input", new CLIInputWin(label, message, inputs, cursorPos, displayPos, width, index))
		activewin = window.length - 1
		drawWindows()
	}


	// ------- List Window Methods -------

	/**
	  * Adds a "list" window to the terminal which is accessible in window mode.
	  *
	  * @param label		Window label.  (Must be unique among windows.)
	  * @param items		Items listed in the window.
	  * @param x			Left position of the upper-left corner of the window.  (minimum of 1)
	  * @param y			Top position of the upper-left corner of the window.  (minimum of 1)
	  * @param width		Width of the content within the window.  (minimum of 1)
	  * @param height		Height of the content of within the window.  (minimum of 1)
	  * @param unselected	Selected item formatting string.  (default = `ansiBkgCyan + ansiTxtBlack`)
	  * @param selected		Unselected item formatting string.  (default = `ansiBkgCyanL + ansiTxtBlack`)
	  * @param borderStyle	Window border formatting string.  (default = `ansiBkgDefault + ansiTxtWhite`)
	  * @param labelStyle	Window label formatting string for when window is not active.  (default = `ansiBkgDefault + ansiTxtWhite`)
	  * @param activeStyle	Window label formatting string for when window is active.  (default = `ansiBkgWhite + ansiTxtBlack`)
	  * @return				Returns whether adding the window was successful.
	  */
	def addList (
		label:			String,
		items:			List[String],
		index:			Int = -1,
		x:				Int,
		y:				Int,
		width:			Int,
		height:			Int,
		unselected:		String = ansiBkgCyan + ansiTxtBlack,
		selected:		String = ansiBkgCyanL + ansiTxtBlack,
		borderStyle:	String = ansiBkgDefault + ansiTxtWhite,
		labelStyle:		String = ansiBkgDefault + ansiTxtWhite,
		activeStyle:	String = ansiBkgWhite + ansiTxtBlack
	): Boolean = {
		if (window.find(_.asInstanceOf[CLIWinObj].label() == label) == None) {  // Add new window
			val index1 = index.max(-1).min(items.length - 1)
			val x1 = x.max(1)
			val y1 = y.max(1)
			val width1 = width.max(1)
			val height1 = height.max(1)
			window :+= new CLIWinObj("list", new CLIListWin(label, items, index1, x1, y1, width1, height1, unselected, selected, borderStyle, labelStyle, activeStyle))
			true
		} else  // Window with that label already exists
			false
	}

	/**
	  * Adds a "list" window to the terminal which is accessible in window mode.
	  *
	  * @param win	CLIWindow object to add.
	  * @return		Returns whether adding the window was successful.
	  */
	def addList (win: CLIListWin): Boolean = {
		if (window.find(_.asInstanceOf[CLIWinObj].label() == win.label) == None) {  // Add new window
			var newwin = new CLIListWin(win.label, win.items, win.index.max(-1).min(win.items.length - 1), win.x.max(1), win.y.max(1), win.width.max(1), win.height.max(1), win.unselected, win.selected, win.borderStyle, win.labelStyle, win.activeStyle)
			window :+= new CLIWinObj("list", newwin)
			true
		} else  // Window with that label already exists
			false
	}

	/**
	  * Deletes a window from the terminal.
	  *
	  * @param label	The label of the window to delete.
	  * @return			Returns whether deleting the window was successful.
	  */
	def deleteWin (label: String): Boolean = {
		val idx = window.indexWhere(_.asInstanceOf[CLIWinObj].label() == label)
		if (idx > -1) {  // Delete a window
			if (activewin == idx)  // Switch to "no active window" mode
				activewin = -1
			window.remove(idx)
			if ((activewin == -1) && (mode == "window"))  // Switch out of "window" mode if an active window isn't selected
				mode = "text"
			true
		} else
			false
	}

	/**
	  * Draw the windows.
	  */
	private def drawWindows (): Unit = {
		// termResize()
		var screen = ArrayBuffer.fill[String](termHeight - 2)(backgroundColor + (" " * (termWidth - 1)))  // Fill array with blank lines
		var row = 0
		var col = 0
		var minWidth = 1
		if (window.length > 0) {
			for(win <- window) {
				win.objType match {
					case "message" => {  // Draw a message window
						val winObj = win.obj.asInstanceOf[CLIMsgWin]
						var maxWidth = winObj.maxWidth
						if (maxWidth == -1 || maxWidth > termWidth - 6)
							maxWidth = termWidth - 6
						var tmpStr = winObj.message
						if (tmpStr.indexOf("\n") > -1)  // Turn any line breaks into a single "word"
							tmpStr = tmpStr.replaceAll("\n", " \n ")
						val words = tmpStr.split(" ")
						var lines = ArrayBuffer.empty[String]
						var line = ""
						var tmpWord = ""
						for(word <- words) {  // Build up lines to display
							if (word.length > maxWidth)  // Trim words wider than the maximum line width
								tmpWord = word.substring(0, maxWidth - 1) + "…"
							else
								tmpWord = word
							if (tmpWord == "\n") {  // Change \n's to new lines
								lines :+= line
								line = ""
							} else if (line == "")
								line = tmpWord
							else if (line.length + tmpWord.length + 1 > maxWidth) {
								lines :+= line
								line = tmpWord
							} else
								line += " " + tmpWord
						}
						if (line != "")
							lines :+= line
						winObj.numLines = lines.length  // Number of lines of text
						winObj.maxVisibleLines = winObj.numLines  // Number of visible lines of text
						if (winObj.maxVisibleLines > termHeight - 7)
							winObj.maxVisibleLines = termHeight - 7
						winObj.maxScroll = winObj.numLines - winObj.maxVisibleLines  // Maximum scroll position
						if (winObj.scrollPos > winObj.maxScroll)
							winObj.scrollPos = winObj.maxScroll
						val height = winObj.numLines + 4  // Window height
						row = (termHeight - height).max(2) / 2
						col = (termWidth - maxWidth - 4) / 2
						if ((win == window(activewin)) && (mode == "window"))  // Top line of the window
							line = winBkgColor + winTxtColor + "┌─" + ansiInvert + " " + winObj.label + " " + ansiNotInverted + ("─" * (maxWidth - winObj.label.length - 1)) + "┐"
						else
							line = winBkgColor + winTxtColor + "┌─ " + winObj.label + " " + ("─" * (maxWidth - winObj.label.length - 1)) + "┐"
						screen(row) = screen(row).ansiReplaceString(line, col)  // Header
						row += 1
						if (winObj.scrollPos > 0) {  // Show "Scroll up" indicator on the second line
							var halfWidth: Int = maxWidth / 2
							if (maxWidth % 2 == 0)
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▲▲" + (" " * halfWidth) + "│"
							else {
								halfWidth += 1
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▲" + (" " * halfWidth) + "│"
							}
						} else
							line = winBkgColor + winTxtColor + "│" + (" " * (maxWidth + 2)) + "│"  // Second line
						screen(row) = screen(row).ansiReplaceString(line, col)  // Top gap
						row += 1
						for (i <- winObj.scrollPos to winObj.maxVisibleLines + winObj.scrollPos - 1) {  // Loop to draw lines of message text
							line = winBkgColor + winTxtColor + "│ " + lines(i) + (" " * (maxWidth - lines(i).length + 1)) + "│"
							screen(row) = screen(row).ansiReplaceString(line, col)  // Message text
							row += 1
						}
						if (winObj.scrollPos < winObj.maxScroll) {  // Show "Scroll down" indicator on the second to last line
							var halfWidth: Int = maxWidth / 2
							if (maxWidth % 2 == 0)
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▼▼" + (" " * halfWidth) + "│"
							else {
								halfWidth += 1
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▼" + (" " * halfWidth) + "│"
							}
						} else
							line = winBkgColor + winTxtColor + "│" + (" " * (maxWidth + 2)) + "│"  // Second to last line
						screen(row) = screen(row).ansiReplaceString(line, col)  // Bottom gap
						row += 1
						line = winBkgColor + winTxtColor + "└" + ("─" * (maxWidth + 2)) + "┘"  // Bottom line
						screen(row) = screen(row).ansiReplaceString(line, col)  // Footer
					}
					case "input" => {  // Draw an input window
						val winObj = win.obj.asInstanceOf[CLIInputWin]
						var interiorWidth = winObj.width
						if (interiorWidth == -1 || interiorWidth > termWidth - 6)
							interiorWidth = termWidth - 6
						var height = (winObj.items.size * 2) + 4  // Window height
						if (winObj.message != "")
							height += 2
						var line = ""
						var tmpStr = ""
						row = (termHeight - height).max(2) / 2
						col = (termWidth - interiorWidth - 4) / 2
						if ((win == window(activewin)) && (mode == "window"))  // Top line of the window
							line = winBkgColor + winTxtColor + "┌─" + ansiInvert + " " + winObj.label + " " + ansiNotInverted + ("─" * (interiorWidth - winObj.label.length - 1)) + "┐"
						else
							line = winBkgColor + winTxtColor + "┌─ " + winObj.label + " " + ("─" * (interiorWidth - winObj.label.length - 1)) + "┐"
						screen(row) = screen(row).ansiReplaceString(line, col)  // Header
						row += 1
						if (winObj.scrollPos > 0) {  // Show "Scroll up" indicator on the second line
							var halfWidth: Int = interiorWidth / 2
							if (interiorWidth % 2 == 0)
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▲▲" + (" " * halfWidth) + "│"
							else {
								halfWidth += 1
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▲" + (" " * halfWidth) + "│"
							}
						} else
							line = winBkgColor + winTxtColor + "│" + (" " * (interiorWidth + 2)) + "│"  // Second line
						screen(row) = screen(row).ansiReplaceString(line, col)  // Top gap
						row += 1
						if (winObj.message != "") {  // Draw top message text
							line = winBkgColor + winTxtColor + "│ " + winObj.message + (" " * (interiorWidth - winObj.message.length + 1)) + "│"  // Message text
							screen(row) = screen(row).ansiReplaceString(line, col)
							row += 1
							line = winBkgColor + winTxtColor + "│" + (" " * (interiorWidth + 2)) + "│"  // Blank line
							screen(row) = screen(row).ansiReplaceString(line, col)
							row += 1
						}
						var idx = -1
						var passworded = false
						for (i <- winObj.items) {  // Loop to draw input labels and lines  // *** Add window scroll support
							idx += 1  // Track input index
							if (i._1.indexOf("~") == 0) {
								passworded = true
								line = winBkgColor + winTxtColor + "│ " + i._1.substring(1) + (" " * (interiorWidth - i._1.length() + 2)) + "│"  // Draw the input label minus the "~" at the front
							} else {
								passworded = false
								line = winBkgColor + winTxtColor + "│ " + i._1 + (" " * (interiorWidth - i._1.length() + 1)) + "│"  // Draw the input label
							}
							screen(row) = screen(row).ansiReplaceString(line, col)  // Input label
							row += 1
							if (passworded)
								tmpStr = "●" * i._2.length()  // Obscure text
							else
								tmpStr = i._2
							tmpStr = tmpStr.substring(winObj.displayPos(idx), tmpStr.length().min(interiorWidth - 2))  // Input string truncated to fit input area starting at displayPos
							if (winObj.index == idx) {  // Sets input position and shows cursor
								winObj.inputX = col + 3
								winObj.inputY = row + 2
								tmpStr = curstring(tmpStr, winObj.cursorPos(idx) - winObj.displayPos(idx))  // Input string with cursor
							}
							tmpStr = ansiBkgDefault + ansiTxtDefault + tmpStr
							line = winBkgColor + winTxtColor + "│  " + tmpStr + (" " * (interiorWidth - tmpStr.ansiStrip.length() - 2)) + winBkgColor + winTxtColor + "  │"  // Draw the input label:
							screen(row) = screen(row).ansiReplaceString(line, col)  // Input area
							row += 1
							if (i._1 != winObj.items.last._1) {
								line = winBkgColor + winTxtColor + "│" + (" " * (interiorWidth + 2)) + "│"  // Blank line
								screen(row) = screen(row).ansiReplaceString(line, col)
								row += 1
							}
						}
						if (winObj.scrollPos < winObj.maxScroll) {  // Show "Scroll down" indicator on the second to last line
							var halfWidth: Int = interiorWidth / 2
							if (interiorWidth % 2 == 0)
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▼▼" + (" " * halfWidth) + "│"
							else {
								halfWidth += 1
								line = winBkgColor + winTxtColor + "│" + (" " * halfWidth) + "▼" + (" " * halfWidth) + "│"
							}
						} else
							line = winBkgColor + winTxtColor + "│" + (" " * (interiorWidth + 2)) + "│"  // Second to last line
						screen(row) = screen(row).ansiReplaceString(line, col)  // Bottom gap
						row += 1
						line = winBkgColor + winTxtColor + "└─" + ("─" * (interiorWidth - 14))  // Bottom line
						if (winObj.index == -2)  // "OK" selected
							line += ansiInvert + " OK " + ansiNotInverted + "──"
						else
							line += " OK ──"
						if (winObj.index == -1)  // "Cancel" selected
							line += ansiInvert + " Cancel " + ansiNotInverted + "─┘"
						else
							line += " Cancel ─┘"
						screen(row) = screen(row).ansiReplaceString(line, col)  // Footer
					}
					case _ =>  // Unknown window type
				}
			}
		}
		var scrStr = ""
		for (i <- 0 to screen.length - 1) {  // Build screen string
			scrStr += ansiCursorPos(1, i + 1) + ansiReset + screen(i)
			if (scrStr.length > 6000) {
				print(ansiCursorSave + scrStr + ansiCursorLoad)  // Update screen
				scrStr = ""
			}
		}
		if (scrStr.length > 0)
			print(ansiCursorSave + scrStr + ansiCursorLoad)  // Update screen
	}


	// ======= Text Methods =======

	/**
	  * Draw the text input.
	  *
	  * @param nocursor	Set this to `true` to hide the cursor.  (default = `false`)
	  */
	private def drawText (nocursor: Boolean = false): Unit = {
		var text = inputStr
		if (password)
			text = "●" * ansiStrip(inputStr).length
		if (mode != "text" || nocursor || Calendar.getInstance().get(Calendar.MILLISECOND) * 2 > 500)
			print(ansiCursorSave + ansiCursorPos(1, terminal.getHeight - 1) + ansiReset + prompt + ansiReset + ansiClrLnTail + text + " " + ansiCursorLoad)
		else
			print(ansiCursorSave + ansiCursorPos(1, terminal.getHeight - 1) + ansiReset + prompt + ansiReset + ansiClrLnTail + curstring(text, inputPos) + " " + ansiCursorLoad)
	}


	// ======= Menu Methods =======

	/**
	  * Adds an array as a menu column.  All menu items must be unique.  The first item in the array will act as a menu title.
	  *
	  * @param items	A menu title and menu entries.  Array must be greater than one item long.
	  * @param redraw	Whether the terminal should redraw immediately.  (default = `true`)
	  * @return			Returns whether adding the menu was successful.
	  */
	def addMenu (items: Array[String], redraw: Boolean = true): Boolean = {
		if (items.length > 1) {  // *** Make sure there are no duplicate menu items within this or across other menus
			menu :+= items
			if (redraw)
				drawMenu()
			true
		} else  // Sequence is too small to be used for a menu
			false
	}

	/**
	  * Updates the menu column that has a matching first element.
	  *
	  * @param items	The new version of the menu with the matching first element.
	  * @param redraw	Whether the terminal should redraw immediately.  (default = `true`)
	  * @return			Returns whether deleting the menu was successful.
	  */
	def updateMenu (items: Array[String], redraw: Boolean = true): Boolean = {
		if (menu.length > 0 && items.length > 1) {
			val title = items(0)
			val idx = menu.indexWhere(_(0) == title)
			if (idx < 0)  // No match
				false
			else {  // Replace the matching menu  // *** Make sure there are no duplicate menu items within this or across other menus
				menu(idx) = items
				if (redraw)
					drawMenu()
				true
			}
		} else  // No menu or sequence is too small to be used for a menu
			false
	}

	/**
	  * Updates the text of a menu item.
	  *
	  * @param oldItem	Name of the existing menu item.
	  * @param newItem	Name to change the existing menu item to.  Must be unique in the menu.
	  * @param redraw	Whether the terminal should redraw immediately.  (default = `true`)
	  * @return			Returns whether updating the menu item was successful.
	  */
	def updateMenuItem (oldItem: String, newItem: String, redraw: Boolean = true): Boolean = {
		if (menu.length > 0) {  // *** Make sure that the new item name doesn't already exist in a menu
			var m = 0
			var n = -1
			while (n == -1) {  // Look for menu item matching oldItem
				n = menu(m).indexWhere(_ == oldItem)
				if (n == -1) {
					m += 1
					if (m >= menu.length) {
						m = -1
						n = 0
					}
				}
			}
			if (m == -1)  // No match
				false
			else {  // Replace the matching menu
				menu(m)(n) = newItem
				if (redraw)
					drawMenu()
				true
			}
		} else  // No menu
			false
	}

	/**
	  * Deletes the menu column which has a matching title.
	  *
	  * @param title	Title of the menu column to delete.
	  * @param redraw	Whether the terminal should redraw immediately.  (default = `true`)
	  * @return			Returns whether deleting the menu was successful.
	  */
	def deleteMenu (title: String, redraw: Boolean = true): Boolean = {
		if (menu.length > 0) {
			val idx = menu.indexWhere(_(0) == title)
			if (idx < 0)  // No match
				false
			else {  // Remove the matching menu
				menu.remove(idx)
				if (redraw)
					drawMenu()
				true
			}
		} else  // No menu
			false
	}

	/**
	  * Draw the menu.
	  *
	  * @param redrawWindows Whether or not to redraw the windows, if any windows exist, before drawing the menus.
	  */
	private def drawMenu (redrawWindows: Boolean = true): Unit = {
		if (menu.length > 0) {
			var head = ""
			var menubar = ""
			var line = ""
			var maxwidth = 0
			var dif = 0
			/*
			if (redrawWindows || window.length == 0) {  // Clear any old menu lines, then redraw the windows, before drawing the menus
				while (maxRows >= 0) {
					head += ansiCursorPos(1, maxRows + 2) + ansiClrLn  // *** Clear out old menus better?
					maxRows -= 1
				}
			}
			*/
			if (redrawWindows)
				drawWindows()  // Refresh windows below the menu
			for(i <- 0 to menu.length - 1) {
				menubar +=  "⌠"  // "∫" doesn't display correctly in a DOS command window
				if (i == menuCol) {  // Draw menu rows
					val left = ansiStrip(menubar).length
					for (n <- 1 to menu(i).length - 1) {  // Find the longest menu item
						if (maxwidth < ansiStrip(menu(i)(n)).length)
							maxwidth = ansiStrip(menu(i)(n)).length
					}
					for (n <- 1 to menu(i).length - 1) {
						line += ansiCursorPos(left, n + 1)
						dif = maxwidth - menu(i)(n).length + 1
						if (n - 1 == menuRow)
							line += "│ " + ansiInvert + " " + menu(i)(n) + (" " * dif) + ansiNotInverted + " │"
						else
							line += "│  " + menu(i)(n) + (" " * dif) + " │"
					}
					line += ansiCursorPos(left, menu(i).length + 1) + "└" + ("─" * (maxwidth + 4)) + "┘"
					maxRows = menu(i).length - 1
					menubar += ansiInvert + " " + menu(i)(0) + " " + ansiNotInverted
				} else
					menubar += " " + menu(i)(0) + " "
				menubar += "\\_"
			}
			val len = terminal.getWidth() - ansiStrip(menubar).length - 1
			if (len > 0)
				menubar += "_" * len
			print(ansiCursorSave + head + ansiCursorPos(1, 1) + ansiReset + ansiClrLn + menuColor + menubar + line + ansiCursorLoad)
		}
	}


	// ======= Terminal Methods =======

	/**
	  * Initialize the CLITool terminal, clear the screen, and set the cursor at the bottom-left corner of the terminal.
	  * - Should be done before anything else when using the CLITool.
	  * - You must use the `CLITool.close()` method to close the terminal before exiting the application.
	  */
	def init (): org.jline.terminal.Terminal = {
		if (instance == 0) {  // Prevent duplicate instances from running
			instance += 1
			terminal = TerminalBuilder.builder().jna(true).system(true).build()
			terminal.enterRawMode()
			reader = terminal.reader()
			termWidth = terminal.getWidth()
			termHeight = terminal.getHeight()
			print(ansiResetFull)  // Clear screen and initialize cursor position to bottom-left
		}
		terminal
	}

	/**
	  * Checks to see if the terminal window size has changed and updates terminal size tracking variables.
	  *
	  * @return	Whether the terminal window size has changed.
	  */
	private def termResize(): Boolean = {
		if (termWidth != terminal.getWidth() || termHeight != terminal.getHeight().min(maxHeight)) {
			print(s"${ansiResetLn}Terminal size changed from ${termWidth}x$termHeight to ${terminal.getWidth()}x${terminal.getHeight()}")
			termWidth = terminal.getWidth()
			termHeight = terminal.getHeight().min(maxHeight)
			true
		} else
			false
	}

	/**
	  * Update the contents of the terminal window.
	  */
	def termUpdate (fullRefresh: Boolean = false): Unit = {
		if (termResize() || fullRefresh)
			print(ansiClrScr + ansiCursorPos(1, terminal.getHeight))
		drawWindows()	// Lowest draw priority
		drawText()		// Medium draw priority
		drawMenu(false)	// Highest draw priority
	}

	/**
	  * Get a command string from the user via the terminal.
	  *
	  * @return Command string from user input.
	  */
	def getCommand (): Command = {
		if (instance == 0)  // Initialize terminal
			init()
		val charkeys = List.range(32, 127)  // Characters from " " to "~"
		var continue = true
		var keycmd = ""
		var output = ""
		var input = 0
		var command = ""
		var args = ArrayBuffer.empty[String]
		var sargs = ArrayBuffer.empty[String]
		var index = -1
		var lastmode = mode
		inputStr = ""
		inputPos = 0

		if (mode == "text")  // Display the prompt
			print(ansiCursorSave + ansiCursorPos(1, terminal.getHeight - 1) + ansiReset + ansiClrLn + prompt + ansiMarker + ansiCursorLoad)
		termUpdate()
		while (continue) {  // Read user input
			if (termResize())  // Redraw the screen if the terminal window's size has changed.
				termUpdate()
			input = reader.read(50)
			lastmode = mode
			mode match {
				case "text" => {	// Text input mode
					input match {
						case -2	=> drawText()  // No input; used for cursor blink
						case 0	=>		// CTRL+SPACE
						case 1	=>		// CTRL+a
						case 2	=>		// CTRL+b
						case 4	=>		// CTRL+d
						case 7	=>		// CTRL+g
						case 8	=> {	// CTRL+h / SHIFT+BACKSPACE keys
							password = !password  // Toggle "password" setting for text mode to hide input text
							drawText()
							if (password)
								print(ansiResetLn + s"Hidden text mode: ${ansiBold}ON${ansiNormalIntesity} ")
							else
								print(ansiResetLn + s"Hidden text mode: ${ansiBold}OFF${ansiNormalIntesity} ")
						}
						case 9 	=>		// CTRL+i / TAB key
						case 12	=>		// CTRL+l
						case 10 | 13	=> {	// ENTER key; end the loop
							inputPos = -1
							inputStr = inputStr.trim()
							drawText(true)
							if (!password) {  // It's not a "password" so it can be added to the command history
								if (inputStr != "")  // Add string to history
									history :+= inputStr
								while (history.length > historymax)  // Limit history size
									history.remove(0)
								historypos = history.length
							}
							var tmpStr = ""
							if (inputStr.indexOf(" ") == -1)  // Get the "command" (the first word in the string)
								command = inputStr
							else {
								command = inputStr.substring(0, inputStr.indexOf(" "))
								tmpStr = inputStr.substring(inputStr.indexOf(" ") + 1, inputStr.length).trim()
							}
							while (tmpStr != "") {  // Get arguments from the string
								if (tmpStr.indexOf("\"") == 0) {  // Get string within double-quotes
									if (tmpStr.indexOf("\"", 1) == -1) {  // Get the remainder of the string
										args :+= tmpStr.substring(1)
										tmpStr = ""
									} else {  // Get the rest of the text within double-quotes
										args :+= tmpStr.substring(1, tmpStr.indexOf("\"", 1))
										tmpStr = tmpStr.substring(tmpStr.indexOf("\"", 1) + 1, tmpStr.length).trim()
									}
								} else if (tmpStr.indexOf(" ") > -1) {  // Add next argument to the list
									args :+= tmpStr.substring(0, tmpStr.indexOf(" "))
									tmpStr = tmpStr.substring(tmpStr.indexOf(" ") + 1, tmpStr.length).trim()
								} else {  // Add the last argument to the list
									args :+= tmpStr
									tmpStr = ""
								}
								sargs :+= sanitizeString(args.last)
							}
							print(ansiResetLn + ansiScrollUp(1))
							continue = false  // Exit the loop
						}
						case 14	=>		// CTRL+n
						case 15	=>		// CTRL+o
						case 18	=> {	// CTRL+r  (force redraw)
							termUpdate(true)
							print(ansiResetLn + "Forced screen refresh. ")
						}
						case 19	=>		// CTRL+s
						case 20	=>		// CTRL+t
						case 21	=>		// CTRL+u
						case 23	=>		// CTRL+BACKSPACE keys
						case 24	=>		// CTRL+x
						case 25	=>		// CTRL+y
						case 27	=> {	// ESC sequence started
							keycmd = ""
							while (reader.peek(20) > 0)
								keycmd += reader.read().toChar  // Read in any pending inputs
							keycmd match {
								case ""		=>		// ESC key
								case "d"	=>		// CTRL+DELETE keys
								case "[A" | "OA" => {	// UP key
									if (history.length > 0) {
										if (historypos > 0)
											historypos -= 1
										inputStr = history(historypos)
										inputPos = inputStr.length
									}
									drawText()
								}
								case "[1;2A"=>		// SHIFT+UP keys
								case "[1;5A"=>		// CTRL+UP keys
								case "[B" | "OB" => {	// DOWN key
									if (history.length > 0) {
										if (historypos < history.length)
											historypos += 1
										if (historypos < history.length)
											inputStr = history(historypos)
										else
											inputStr = ""
										inputPos = inputStr.length
									}
									drawText()
								}
								case "[1;2B"=>		// SHIFT+DOWN keys
								case "[1;5B"=>		// CTRL+DOWN keys
								case "[C" | "OC" => {	// RIGHT key
									if (inputPos < inputStr.length)
										inputPos += 1
									drawText()
								}
								case "[1;2C"=>		// SHIFT+RIGHT keys
								case "[1;5C"=>		// CTRL+RIGHT keys
								case "[D" | "OD" => {	// LEFT key
									if (inputPos > 0)
										inputPos -= 1
									drawText()
								}
								case "[1;2D"=>		// SHIFT+LEFT keys
								case "[1;5D"=>		// CTRL+LEFT keys
								case "[F" | "[4~" | "[8~" => {  // END key
									inputPos = inputStr.length
									drawText()
								}
								case "[H" | "[1~" | "[7~" => {  // HOME key
									inputPos = 0
									drawText()
								}
								case "[Z"	=>		// SHIFT+TAB keys
								case "[2~"	=> {	// INSERT key; mode switch key
									if (menu.length > 0) {  // Switch to "menu" mode
										mode = "menu"
										menuCol = 0
										menuRow = 0
										termUpdate()
										print(ansiResetLn + mode.toUpperCase + " mode. ")
									} else if (window.length > 0) {  // Switch to "window" mode
										mode = "window"
										termUpdate()
										print(ansiResetLn + mode.toUpperCase + " mode. ")
									}
								}
								case "[3~"	=> {	// DELETE key
									if (inputStr.length > 0 && inputPos < inputStr.length) {
										inputStr = inputStr.substring(0, inputPos) + inputStr.substring(inputPos + 1, inputStr.length)
										drawText()
									}
								}
								case "[3:2~"=>		// SHIFT+DELETE keys
								case "[5~"	=>		// PAGE UP key
								case "[6~"	=>		// PAGE DOWN key
								case _		=> print("Unknown escape sequence: (" + keycmd + ") ")
							}
						}
						case 29	=>		// CTRL+]
						case 31	=>		// CTRL+/
						case 127 => {	// BACKSPACE key
							if (inputPos > 0 && inputStr.length > 0) {
								inputStr = inputStr.substring(0, inputPos - 1) + inputStr.substring(inputPos, inputStr.length)
								inputPos -= 1
								drawText()
							}
						}
						case key if (charkeys.contains(key)) => {  // A printable character was entered
							inputStr = inputAdd(input.toChar, inputStr, inputPos)
							inputPos += 1
							drawText()
						}
						case key if (key < 32) =>
							print(ansiResetLn + s"You pressed an unknown key: ($key) ")
						case key if (key >= 32) =>
							print(ansiResetLn + "You pressed an unknown key: " + key.toChar + s" ($key) ")
					}
				}
				case "menu" => {	// Menu input mode
					input match {
						case -2	=> 		// No input
						case 0	=>		// CTRL+SPACE
						case 1	=>		// CTRL+a
						case 2	=>		// CTRL+b
						case 4	=>		// CTRL+d
						case 7	=>		// CTRL+g
						case 8	=>		// CTRL+h / SHIFT+BACKSPACE keys
						case 9 	=>		// CTRL+i / TAB key
						case 12	=>		// CTRL+l
						case 10 | 13 | 32 => {	// ENTER or SPACE key
							inputStr = menu(menuCol)(menuRow + 1)
							menuCol = -1
							menuRow = -1
							mode = "empty"  // Exit "menu" mode
							termUpdate()
							mode = "text"
							print(ansiResetLn + ansiScrollUp(1))
							continue = false  // Exit the loop
						}
						case 14	=>		// CTRL+n
						case 15	=>		// CTRL+o
						case 18	=> {	// CTRL+r  (force redraw)
							termUpdate(true)
							print(ansiResetLn + "Forced screen refresh. ")
						}
						case 19	=>		// CTRL+s
						case 20	=>		// CTRL+t
						case 21	=>		// CTRL+u
						case 23	=>		// CTRL+BACKSPACE keys
						case 24	=>		// CTRL+x
						case 25	=>		// CTRL+y
						case 27	=> {	// ESC key; escape sequence started
							keycmd = ""
							while (reader.peek(20) > 0)
								keycmd += reader.read().toChar  // Read in any pending inputs
							keycmd match {
								case ""		=>		// ESC key
								case "d"	=>		// CTRL+DELETE keys
								case "[A" | "OA" => {	// UP key
									menuRow -= 1
									if (menuRow < 0)
										menuRow = menu(menuCol).length - 2
									termUpdate()
								}
								case "[1;2A"=>		// SHIFT+UP keys
								case "[1;5A"=>		// CTRL+UP keys
								case "[B" | "OB" => {	// DOWN key
									menuRow += 1
									if (menuRow >= menu(menuCol).length - 1)
										menuRow = 0
									termUpdate()
								}
								case "[1;2B"=>		// SHIFT+DOWN keys
								case "[1;5B"=>		// CTRL+DOWN keys
								case "[C" | "OC" => {	// RIGHT key
									menuCol += 1
									if (menuCol >= menu.length)
										menuCol = 0
									menuRow = 0
									termUpdate()
								}
								case "[1;2C"=>		// SHIFT+RIGHT keys
								case "[1;5C"=>		// CTRL+RIGHT keys
								case "[D" | "OD" => {	// LEFT key
									menuCol -= 1
									if (menuCol < 0)
										menuCol = menu.length - 1
									menuRow = 0
									termUpdate()
								}
								case "[1;2D"=>		// SHIFT+LEFT keys
								case "[1;5D"=>		// CTRL+LEFT keys
								case "[F" | "[4~" | "[8~" =>	// END key
								case "[H" | "[1~" | "[7~" =>	// HOME key
								case "[Z"	=>		// SHIFT+TAB keys
								case "[2~"	=> {	// INSERT key; mode switch key
									menuCol = -1
									menuRow = -1
									if (window.length > 0) {  // Switch to "window" mode
										mode = "window"
										termUpdate()
										print(ansiResetLn + mode.toUpperCase + " mode. ")
									} else {  // Switch to "text" mode
										mode = "text"
										termUpdate()
										print(ansiResetLn + mode.toUpperCase + " mode. ")
									}
								}
								case "[3~"	=>		// DELETE key
								case "[3:2~"=>		// SHIFT+DELETE keys
								case "[5~"	=>		// PAGE UP key
								case "[6~"	=>		// PAGE DOWN key
								case _		=> print(ansiResetLn + "Unknown escape sequence: (" + keycmd + ") ")
							}
						}
						case 29	=>		// CTRL+]
						case 31	=>		// CTRL+/
						case 127 =>		// BACKSPACE key
						case key if (charkeys.contains(key)) =>		// A printable character was entered
						case key if (key < 32) =>
							print(ansiResetLn + s"You pressed an unknown key: ($key) ")
						case key if (key >= 32) =>
							print(ansiResetLn + "You pressed an unknown key: " + key.toChar + s" ($key) ")
					}
				}
				case "window" => {	// Window input mode
					if ((activewin < 0 || activewin >= window.length) && window.length > 0)
						activewin = 0
					else if (window.length == 0)
						activewin = -1
					if (activewin != -1) {
						window(activewin).objType match {
							case "message"	=> {  // "message" window handler
								if (input != -2) {  // Some input occurred
									keycmd = ""
									if (input == 27) {  // Possible ESC code
										while (reader.peek(20) > 0)
											keycmd += reader.read().toChar  // Read in any pending inputs from an ESC code
									}
									if (List("[A", "[B", "[5~", "[6~", "[F", "[4~", "[8~", "[H", "[1~", "[7~").indexOf(keycmd) > -1) {  // UP, DOWN, PAGE UP, PAGE DOWN, END, and HOME scrolling
										var msgObj = window(activewin).obj.asInstanceOf[CLIMsgWin]
										var oldPos = msgObj.scrollPos
										keycmd match {
											case "[A"	=> msgObj.scrollPos = (msgObj.scrollPos - 1).max(0)  // UP key
											case "[B"	=> msgObj.scrollPos = (msgObj.scrollPos + 1).min(msgObj.maxScroll)  // DOWN key
											case "[5~"	=> msgObj.scrollPos = (msgObj.scrollPos - msgObj.maxVisibleLines).max(0)  // PAGE UP key
											case "[6~"	=> msgObj.scrollPos = (msgObj.scrollPos + msgObj.maxVisibleLines).min(msgObj.maxScroll)  // PAGE DOWN key
											case "[F" | "[4~" | "[8~" => msgObj.scrollPos = msgObj.maxScroll  // END key
											case "[H" | "[1~" | "[7~" => msgObj.scrollPos = 0  // HOME key
										}
										if (oldPos != msgObj.scrollPos)
											termUpdate()
									} else if (input == 9 && !window(activewin).locked() && window.length > 1) {  // Handle TAB changing window focus
										activewin += 1
										if (activewin >= window.length)
											activewin = 0
										termUpdate()
									} else {  // Close the message window
										inputStr = ""
										window.remove(activewin)
										activewin = -1
										mode = "empty"  // Exit "window" mode
										termUpdate()
										mode = "text"
										print(ansiResetLn + ansiScrollUp(1))
										continue = false  // Exit the loop
									}
								}
							}
							case "input"	=> {
								var winOb = window(activewin).obj.asInstanceOf[CLIInputWin]
								input match {
									case -2	=>		// No input
									case 0	=>		// CTRL+SPACE
									case 1	=>		// CTRL+a
									case 2	=>		// CTRL+b
									case 4	=>		// CTRL+d
									case 7	=>		// CTRL+g
									case 8	=>		// CTRL+h / SHIFT+BACKSPACE keys
									case 9 	=> {	// CTRL+i / TAB key
										winOb.index += 1
										if (winOb.index >= winOb.items.size)
											winOb.index = -2
										termUpdate()
									}
									case 12	=>		// CTRL+l
									case 10 | 13 | 32 => {	// ENTER or SPACE key
										if (winOb.index < 0) {  // Activate OK/Cancel button
											inputStr = ""
											if (winOb.index == -2) {
												command = "OK"
												inputStr = command
												for (a <- winOb.items) {  // Get inputs
													args :+= a._2
													sargs :+= sanitizeString(a._2)
													inputStr += " '" + a._2 + "'"
												}
											} else {
												command = "Cancel"
												inputStr = command
											}
											if (activewin > -1 && activewin < window.length)
												window.remove(activewin)
											activewin = -1
											mode = "empty"  // Exit "window" mode
											termUpdate()
											mode = "text"
											print(ansiResetLn)
											continue = false  // Exit the loop
										} else {  // Entering text
											if (input == 32) {  // Add a space to the text
												var key = winOb.items.toIndexedSeq(winOb.index)._1
												winOb.items(key) = inputAdd(input.toChar, winOb.items(key), winOb.cursorPos(winOb.index))
												winOb.cursorPos(winOb.index) += 1
												termUpdate()
											} else {  // Move to next part in input window
												winOb.index += 1
												if (winOb.index >= winOb.items.size)
													winOb.index = -2
												termUpdate()
											}
										}
									}
									case 14	=>		// CTRL+n
									case 15	=>		// CTRL+o
									case 18	=> {	// CTRL+r  (force redraw)
										termUpdate(true)
										print(ansiResetLn + "Forced screen refresh. ")
									}
									case 19	=>		// CTRL+s
									case 20	=>		// CTRL+t
									case 21	=>		// CTRL+u
									case 23	=>		// CTRL+BACKSPACE keys
									case 24	=>		// CTRL+x
									case 25	=>		// CTRL+y
									case 27	=> {	// ESC key; escape sequence started
										keycmd = ""
										while (reader.peek(20) > 0)
											keycmd += reader.read().toChar  // Read in any pending inputs
										keycmd match {
											case ""		=> {	// ESC key  // Cancel the input window
												command = "Cancel"
												inputStr = command
												if (activewin > -1 && activewin < window.length)
													window.remove(activewin)
												activewin = -1
												mode = "empty"  // Exit "window" mode
												termUpdate()
												mode = "text"
												print(ansiResetLn)
												continue = false  // Exit the loop
											}
											case "d"	=>		// CTRL+DELETE keys
											case "[A" | "OA" =>	// UP key
											case "[1;2A"=>		// SHIFT+UP keys
											case "[1;5A"=>		// CTRL+UP keys
											case "[B" | "OB" =>	// DOWN key
											case "[1;2B"=>		// SHIFT+DOWN keys
											case "[1;5B"=>		// CTRL+DOWN keys
											case "[C" | "OC" => {	// RIGHT key
												if (winOb.index >= 0) {  // On an input line
													var key = winOb.items.toIndexedSeq(winOb.index)._1
													if (winOb.cursorPos(winOb.index) < winOb.items(key).length())
														winOb.cursorPos(winOb.index) += 1
													termUpdate()
												} else {  // Set focus on the next input window element
													winOb.index += 1
													if (winOb.index >= winOb.items.size)
														winOb.index = -2
													termUpdate()
												}
											}
											case "[1;2C"=>		// SHIFT+RIGHT keys
											case "[1;5C"=>		// CTRL+RIGHT keys
											case "[D" | "OD" => {	// LEFT key
												if (winOb.index >= 0) {  // On an input line
													if (winOb.cursorPos(winOb.index) > 0)
														winOb.cursorPos(winOb.index) -= 1
													termUpdate()
												} else {  // Set focus on the previous input window element
													winOb.index -= 1
													if (winOb.index < -2)
														winOb.index = winOb.items.size - 1
													termUpdate()
												}
											}
											case "[1;2D"=>		// SHIFT+LEFT keys
											case "[1;5D"=>		// CTRL+LEFT keys
											case "[F" | "[4~" | "[8~" => {	// END key
												if (winOb.index >= 0) {  // On an input line
													var key = winOb.items.toIndexedSeq(winOb.index)._1
													winOb.cursorPos(winOb.index) = winOb.items(key).length()
													termUpdate()
												}
											}
											case "[H" | "[1~" | "[7~" => {	// HOME key
												if (winOb.index >= 0) {  // On an input line
													winOb.cursorPos(winOb.index) = 0
													termUpdate()
												}
											}
											case "[Z"	=>		// SHIFT+TAB keys
											case "[2~"	=> {	// INSERT key; mode switch key
												mode = "text"
												termUpdate()
												print(ansiResetLn + mode.toUpperCase + " mode. ")
											}
											case "[3~"	=> {		// DELETE key
												if (winOb.index >= 0) {  // On an input line
													var key = winOb.items.toIndexedSeq(winOb.index)._1
													if (winOb.items(key).length > 0 && winOb.cursorPos(winOb.index) < winOb.items(key).length) {
														winOb.items(key) = winOb.items(key).substring(0, winOb.cursorPos(winOb.index)) + winOb.items(key).substring(winOb.cursorPos(winOb.index) + 1, winOb.items(key).length)
														termUpdate()
													}
												}
											}
											case "[3:2~"=>		// SHIFT+DELETE keys
											case "[5~"	=>		// PAGE UP key
											case "[6~"	=>		// PAGE DOWN key
											case _		=> print(ansiResetLn + "Unknown escape sequence: (" + keycmd + ") ")
										}
									}
									case 29	=>		// CTRL+]
									case 31	=>		// CTRL+/
									// case 32	=>		// ENTER key
									case 127 => {	// BACKSPACE key
										if (winOb.index >= 0) {  // On an input line
											var key = winOb.items.toIndexedSeq(winOb.index)._1
											if (winOb.cursorPos(winOb.index) > 0 && winOb.items(key).length > 0) {
												winOb.items(key) = winOb.items(key).substring(0, winOb.cursorPos(winOb.index) - 1) + winOb.items(key).substring(winOb.cursorPos(winOb.index), winOb.items(key).length)
												winOb.cursorPos(winOb.index) -= 1
												termUpdate()
											}
										}
									}
									case key if (charkeys.contains(key)) =>	{  // A printable character was entered
										if (winOb.index >= 0) {  // On an input line
											var key = winOb.items.toIndexedSeq(winOb.index)._1
											winOb.items(key) = inputAdd(input.toChar, winOb.items(key), winOb.cursorPos(winOb.index))
											winOb.cursorPos(winOb.index) += 1
											termUpdate()
										}
									}
									case key if (key < 32) =>
										print(ansiResetLn + s"You pressed an unknown key: ($key) ")
									case key if (key >= 32) =>
										print(ansiResetLn + "You pressed an unknown key: " + key.toChar + s" ($key) ")
								}
							}
						}
					}
				}
				/*
				case "dummy" => {  // "Dummy" mode; copy this case to add new modes
					input match {
						case -2	=>		// No input
						case 0	=>		// CTRL+SPACE
						case 1	=>		// CTRL+a
						case 2	=>		// CTRL+b
						case 4	=>		// CTRL+d
						case 7	=>		// CTRL+g
						case 8	=>		// CTRL+h / SHIFT+BACKSPACE keys
						case 9 	=>		// CTRL+i / TAB key
						case 12	=>		// CTRL+l
						case 10 | 13	=>		// ENTER key
						case 14	=>		// CTRL+n
						case 15	=>		// CTRL+o
						case 18	=> {	// CTRL+r  (force redraw)
							termUpdate(true)
							print(ansiResetLn + "Forced screen refresh. ")
						}
						case 19	=>		// CTRL+s
						case 20	=>		// CTRL+t
						case 21	=>		// CTRL+u
						case 23	=>		// CTRL+BACKSPACE keys
						case 24	=>		// CTRL+x
						case 25	=>		// CTRL+y
						case 27	=> {	// ESC key; escape sequence started
							keycmd = ""
							while (reader.peek(20) > 0)
								keycmd += reader.read().toChar  // Read in any pending inputs
							keycmd match {
								case ""		=>		// ESC key
								case "d"	=>		// CTRL+DELETE keys
								case "[A" | "OA" =>	// UP key
								case "[1;2A"=>		// SHIFT+UP keys
								case "[1;5A"=>		// CTRL+UP keys
								case "[B" | "OB" =>	// DOWN key
								case "[1;2B"=>		// SHIFT+DOWN keys
								case "[1;5B"=>		// CTRL+DOWN keys
								case "[C" | "OC" =>	// RIGHT key
								case "[1;2C"=>		// SHIFT+RIGHT keys
								case "[1;5C"=>		// CTRL+RIGHT keys
								case "[D" | "OD" =>	// LEFT key
								case "[1;2D"=>		// SHIFT+LEFT keys
								case "[1;5D"=>		// CTRL+LEFT keys
								case "[F" | "[4~" | "[8~" =>	// END key
								case "[H" | "[1~" | "[7~" =>	// HOME key
								case "[Z"	=>		// SHIFT+TAB keys
								case "[2~"	=> {	// INSERT key; mode switch key
									mode = "modename"
									print(ansiResetLn + mode.toUpperCase + " mode. ")
								}
								case "[3~"	=>		// DELETE key
								case "[3:2~"=>		// SHIFT+DELETE keys
								case "[5~"	=>		// PAGE UP key
								case "[6~"	=>		// PAGE DOWN key
								case _		=> print(ansiResetLn + "Unknown escape sequence: (" + keycmd + ") ")
							}
						}
						case 29	=>		// CTRL+]
						case 31	=>		// CTRL+/
						case 127 =>		// BACKSPACE
						case key if (charkeys.contains(key)) =>		// A printable character was entered
						case key if (key < 32) =>
							print(ansiResetLn + s"You pressed an unknown key: ($key) ")
						case key if (key >= 32) =>
							print(ansiResetLn + "You pressed an unknown key: " + key.toChar + s" ($key) ")
					}
				}
				*/
				case _ => mode = "text"  // Unknown mode, so switch to "text" mode
			}
		}
		new Command(lastmode, inputStr, command, args.toList, sargs.toList, index)
	}

		/*	Likely safe keys:
				Alphanumeric keys = a to z, A to Z, 0 to 9
				Symbol keys = `~!@#$%^&*()-_=+[{]}\|;:'",<.>/?
				CTRL+SPACE = 0
				CTRL+a = 1
				CTRL+b = 2
				CTRL+d = 4
				CTRL+g = 7
				CTRL+h = 8 (SHIFT+BACKSPACE)
				SHIFT+BACKSPACE = 8 (CTRL+h)
				CTRL+i = 9 (TAB)
				TAB = 9 (CTRL+i)
				CTRL+l = 12
				ENTER  = 13
				CTRL+n = 14
				CTRL+o = 15
				CTRL+r = 18
				CTRL+s = 19
				CTRL+t = 20
				CTRL+u = 21
				CTRL+BACKSPACE = 23
				CTRL+x = 24
				CTRL+y = 25
				ESC    = 27 (also used for some terminal "escape sequences")
				CTRL+] = 29
				CTRL+/ = 31
				BACKSPACE = 127

			Unsafe keys:
				Any function keys (F1, F2, etc...) or ALT keys
				CTRL+c = (BREAK)
				CTRL+e = (unsafe - VSCode command: Go to file...)
				CTRL+f = (unsafe - VSCode command: Find)
				CTRL+j = ENTER + (unsafe - VSCode command: Toggle panel)
				CTRL+k = (unsafe - VSCode command: Command code)
				CTRL+m = ENTER + (unsafe - VSCode command: Tab toggle)
				CTRL+p = (unsafe - VSCode command: Go to file...)
				CTRL+q = (unsafe - VSCode command: Open view...)
				CTRL+v = PASTE
				CTRL+w = CTRL+BACKSPACE + (CLOSE WINDOW)
				CTRL+z = (quits sbt)
				CTRL+\ = (JDK thread dump)
				CTRL+TAB = (unsafe - VSCode command: Toggle Editor)
				CTRL+` = (unsafe - VSCode command: Toggle Terminal)
				CTRL+- = (unsafe - VSCode command: Zoom out)
				CTRL+= = (unsafe - VSCode command: Zoom in)
				CTRL+; = (unsafe - VSCode command)
				CTRL+, = (unsafe - VSCode command)
				CTRL+' = ???
				CTRL+. = ???

			Escape sequence keys (likely safe):
				ESC			= ""
				CTRL+DELETE	= "d"
				UP			= "[A" | "OA"
				SHIFT+UP	= "[1;2A"
				CTRL+UP		= "[1;5A"
				DOWN		= "[B" | "OB"
				SHIFT+DOWN	= "[1;2B"
				CTRL+DOWN	= "[1;5B"
				RIGHT		= "[C" | "OC"
				SHIFT+RIGHT	= "[1;2C"
				CTRL+RIGHT	= "[1;5C"
				LEFT		= "[D" | "OD"
				SHIFT+LEFT	= "[1;2D"
				CTRL+LEFT	= "[1;5D"
				END			= "[F" | "[4~" | "[8~"
				HOME		= "[H" | "[1~" | "[7~"
				SHIFT+TAB	= "[Z"
				INSERT		= "[2~"  (Mode Toggle)
				DELETE		= "[3~"
				SHIFT+DELETE= "[3:2~"
				PAGE UP		= "[5~"
				PAGE DOWN	= "[6~"
		*/

	/**
	  * Close the CLITool terminal.
	  * - Must be done before exiting the program.
	  */
	def close (): Unit = {
		if (instance > 0) {  // Ignore if everything is already closed
			instance = 0
			reader.close
			terminal.close
		}
	}
}

/**
  * CLI Tool exception type.  Possible exception triggers:
  * - Calling CLITool.getCommand() without first calling CLITool.init()
  *
  * @param err	The error message to return.
  */
class CLIException (err: String) extends Exception(err) {}
// throw new CLIException("CLITool error.  Error text")