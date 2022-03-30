package com.JeffH001

/**
  * ansiCodes: A collection of useful ANSI codes.  (v1.0 - by JeffH001)
  */
package object ANSICodes {
	import scala.collection.mutable.ArrayBuffer

	/**
	 * ansiCodes exception type.  Possible exception triggers:
	 * - Calling the ansiBkgRGB() or ansiTxtRGB with invalid data
	 *
	 * @param err	The error message to return.
	 */
	class ANSICodeException (err: String) extends Exception(err) {}
	// Example usage:
	// 		throw new ANSICodeException("ANSICodes error: Error text")

	// Cursor codes:
	def ansiUp			(n: Int = 1): String = { "\u001b[" + n + "A" }
	def ansiDown		(n: Int = 1): String = { "\u001b[" + n + "B" }
	def ansiRight		(n: Int = 1): String = { "\u001b[" + n + "C" }
	def ansiLeft		(n: Int = 1): String = { "\u001b[" + n + "D" }
	def ansiNextLn		(n: Int = 1): String = { "\u001b[" + n + "E" }
	def ansiPrvLn		(n: Int = 1): String = { "\u001b[" + n + "F" }
	def ansiCursorPos	(x: Int = 1, y: Int = 1): String = { "\u001b[" + y + ";" + x + "H" }
	def ansiCursorPosX	(x: Int = 1): String = { "\u001b[" + x + "G" }
	def ansiScrollUp	(n: Int = 1): String = { "\u001b[" + n + "S" }
	def ansiScrollDown	(n: Int = 1): String = { "\u001b[" + n + "T" }
	/**
	  * Saves the current cursor position.
	  */
	val ansiCursorSave:	String = "\u001b[s"
	/**
	  * Loads the last saved cursor position.
	  */
	val ansiCursorLoad:	String = "\u001b[u"
	val ansiClrScr:		String = "\u001b[2J"
	val ansiClrScrDown:	String = "\u001b[0J"
	val ansiClrScrUp:	String = "\u001b[1J"
	val ansiClrLn:		String = "\u001b[2K"
	val ansiClrLnHead:	String = "\u001b[1K"
	val ansiClrLnTail:	String = "\u001b[0K"


	// Style codes:
	val ansiBold:			String = "\u001b[1m"
	val ansiFaint:			String = "\u001b[2m"
	/**
	  * Cancels bold and faint.
	  */
	val ansiNormalIntesity:	String = "\u001b[22m"
	val ansiItalic:			String = "\u001b[3m"
	val ansiNotItalic:		String = "\u001b[23m"  // Cancels italic
	val ansiUnderline:		String = "\u001b[4m"
	val ansiNoUnderline:	String = "\u001b[24m"  // Cancels underline
	val ansiBlink:			String = "\u001b[5m"
	val ansiNoBlink:		String = "\u001b[25m"  // Cancels blink
	val ansiInvert:			String = "\u001b[7m"
	val ansiNotInverted:	String = "\u001b[27m"  // Cancels invert
	val ansiInvisible:		String = "\u001b[8m"
	val ansiReveal:			String = "\u001b[28m"  // Cancels invisible
	val ansiStrikethrough:	String = "\u001b[9m"
	val ansiNoStrikethrough:String = "\u001b[29m"  // Cancels strikethrough
	/**
	  * Cancels all style and color changes.
	  */
	val ansiReset:			String = "\u001b[0m"
	/**
	  * Cancels all style and color changes; short alias for ansiReset.
	  */
	val ansiX:				String = "\u001b[0m"


	// Color codes:

	/**
	  * Returns the ANSI string to change text to the given RGB color values.
	  *
	  * @param r	Red value.   (0 to 255; defaults to 255)
	  * @param g	Green value. (0 to 255; defaults to 255)
	  * @param b	Blue value.  (0 to 255; defaults to 255)
	  * @return		ANSI RGB text color code.
	  */
	def ansiTxtRGB (r: Int = 255, g: Int = 255, b: Int = 255): String = {
		"\u001b[38;2;" + r.max(0).min(255) + ";" + g.max(0).min(255) + ";" + b.max(0).min(255) + "m"
	}

	/**
	  * Returns the ANSI string to change text to the given hex RGB color string.
	  *
	  * @param rgbString	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  * @return				ANSI RGB text color code.
	  */
	def ansiTxtRGB (rgbString: String): String = {
		if ((rgbString.length == 7) && (rgbString.substring(0, 1) == "#")) {
			try {
				val r = Integer.parseInt(rgbString.substring(1, 3), 16)
				val g = Integer.parseInt(rgbString.substring(3, 5), 16)
				val b = Integer.parseInt(rgbString.substring(5, 7), 16)
				"\u001b[38;2;" + r + ";" + g + ";" + b + "m"
			} catch {
				case e: Exception => throw new ANSICodeException(s"ANSICodes ansiTxtRGB error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
			}
		} else
			throw new ANSICodeException(s"ANSICodes ansiTxtRGB error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
	}

	/**
	  * Returns the ANSI string to change the background to the given RGB color values.
	  *
	  * @param r	Red value.   (0 to 255; defaults to 0)
	  * @param g	Green value. (0 to 255; defaults to 0)
	  * @param b	Blue value.  (0 to 255; defaults to 0)
	  * @return		ANSI RGB background color code.
	  */
	def ansiBkgRGB (r: Int = 0, g: Int = 0, b: Int = 0): String = {
		"\u001b[48;2;" + r.max(0).min(255) + ";" + g.max(0).min(255) + ";" + b.max(0).min(255) + "m"
	}

	/**
	  * Returns the ANSI string to change the background to the given hex RGB color string.
	  *
	  * @param rgbString	An RGB string using hex values in the form of `"#RRGGBB"` (e.g. `"#0088FF"`).
	  * @return				ANSI RGB text color code.
	  */
	def ansiBkgRGB (rgbString: String): String = {
		if ((rgbString.length == 7) && (rgbString.substring(0, 1) == "#")) {
			try {
				val r = Integer.parseInt(rgbString.substring(1, 3), 16).min(255).max(0)
				val g = Integer.parseInt(rgbString.substring(3, 5), 16).min(255).max(0)
				val b = Integer.parseInt(rgbString.substring(5, 7), 16).min(255).max(0)
				"\u001b[48;2;" + r.max(0).min(255) + ";" + g.max(0).min(255) + ";" + b.max(0).min(255) + "m"
			} catch {
				case e: Exception => throw new ANSICodeException(s"ANSICodes ansiBkgRGB error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
			}
		} else
			throw new ANSICodeException(s"ANSICodes ansiBkgRGB error: Invalid parameter passed '$rgbString'.  Should look more like '#0088FF'.")
	}

	// NOTE: The "L" versions of these colors (e.g. ansiTxtRedL) are the lighter "bright" versions of those colors.
	val ansiTxtDefault:	String = "\u001b[39m"  // Default text color
	val ansiBkgDefault:	String = "\u001b[49m"  // Default background color

	val ansiTxtBlack:	String = "\u001b[30m"
	val ansiBkgBlack:	String = "\u001b[40m"
	val ansiTxtBlackL:	String = "\u001b[90m"
	val ansiBkgBlackL:	String = "\u001b[100m"

	val ansiTxtDkGray:	String = "\u001b[90m"   // a.k.a. "bright black"
	val ansiBkgDkGray:	String = "\u001b[100m"  // a.k.a. "bright black"
	val ansiTxtDkGrey:	String = "\u001b[90m"   // a.k.a. "bright black"; UK spelling
	val ansiBkgDkGrey:	String = "\u001b[100m"  // a.k.a. "bright black"; UK spelling

	val ansiTxtRed:		String = "\u001b[31m"
	val ansiBkgRed:		String = "\u001b[41m"
	val ansiTxtRedL:	String = "\u001b[91m"
	val ansiBkgRedL:	String = "\u001b[101m"

	val ansiTxtGreen:	String = "\u001b[32m"
	val ansiBkgGreen:	String = "\u001b[42m"
	val ansiTxtGreenL:	String = "\u001b[92m"
	val ansiBkgGreenL:	String = "\u001b[102m"

	val ansiTxtYellow:	String = "\u001b[33m"
	val ansiBkgYellow:	String = "\u001b[43m"
	val ansiTxtYellowL:	String = "\u001b[93m"
	val ansiBkgYellowL:	String = "\u001b[103m"

	val ansiTxtBlue:	String = "\u001b[34m"
	val ansiBkgBlue:	String = "\u001b[44m"
	val ansiTxtBlueL:	String = "\u001b[94m"
	val ansiBkgBlueL:	String = "\u001b[104m"

	val ansiTxtMagenta:	String = "\u001b[35m"
	val ansiBkgMagenta:	String = "\u001b[45m"
	val ansiTxtMagentaL:String = "\u001b[95m"
	val ansiBkgMagentaL:String = "\u001b[105m"

	val ansiTxtCyan:	String = "\u001b[36m"
	val ansiBkgCyan:	String = "\u001b[46m"
	val ansiTxtCyanL:	String = "\u001b[96m"
	val ansiBkgCyanL:	String = "\u001b[106m"

	val ansiTxtWhite:	String = "\u001b[37m"
	val ansiBkgWhite:	String = "\u001b[47m"
	val ansiTxtWhiteL:	String = "\u001b[97m"
	val ansiBkgWhiteL:	String = "\u001b[107m"


	// Custom codes:

	/**
	  * Fully clears and resets screen and positions cursor at bottom-left.
	  */
	val ansiResetFull:	String = ansiClrScr + ansiCursorPos(1, 10000) + ansiReset
	/**
	  * Clears current line, resets style and color, and moves cursor to the far left.
	  */
	val ansiResetLn:	String = ansiClrLn + ansiCursorPosX(1) + ansiReset
	/**
	  * Moves up a line, clears it, and sets the cursor position to the far left.
	  */
	val ansiBackLn:		String = ansiUp() + ansiClrLn + ansiCursorPosX()
	/**
	  * An underlined space; useful as a cursor.
	  */
	val ansiMarker:		String = ansiUnderline + " " + ansiNoUnderline


	// Custom functions and classes:

	/**
	  * Descriptor for ANSI style and color codes.
	  *
	  * @param codeStr
	  * @param codeType
	  * @param typeEnable
	  */
	class ANSIElement(codeStr: String, codeType: String, typeDisable: Boolean = false) {
		var code = codeStr
		var ctype = codeType
		var disable = typeDisable
	}

	/**
	  * A list of Regexable ANSI codes organized by type and whether it disables that type.
	  * - Uses the ANSIElement class.
	  */
	val ansiTypesList: List[ANSIElement] = List(
		new ANSIElement("1m", "intensity"),  // Styles
		new ANSIElement("2m", "intensity"),
		new ANSIElement("22m", "intensity", true),
		new ANSIElement("3m", "italic"),
		new ANSIElement("23m", "italic", true),
		new ANSIElement("4m", "underline"),
		new ANSIElement("24m", "underline", true),
		new ANSIElement("5m", "blink"),
		new ANSIElement("25m", "blink", true),
		new ANSIElement("7m", "invert"),
		new ANSIElement("27m", "invert", true),
		new ANSIElement("8m", "invisible"),
		new ANSIElement("28m", "invisible", true),
		new ANSIElement("9m", "strikethrough"),
		new ANSIElement("29m", "strikethrough", true),
		new ANSIElement("3[0-7]m", "txtcolor"),
		new ANSIElement("38;2;[0-9]{1,3};[0-9]{1,3};[0-9]{1,3}m", "txtcolor"),  // Regex match to any 38;2;*;*;*m color
		new ANSIElement("39m", "txtcolor", true),
		new ANSIElement("4[0-7]m", "bkgcolor"),
		new ANSIElement("48;2;[0-9]{1,3};[0-9]{1,3};[0-9]{1,3}m", "bkgcolor"),  // Regex match to any 48;2;*;*;*m color
		new ANSIElement("49m", "bkgcolor", true),
		new ANSIElement("9[0-7]m", "txtcolor"),
		new ANSIElement("10[0-7]m", "bkgcolor"),
		new ANSIElement("0m", "*", true)
	)

	/**
	  * Returns the string stripped of any ANSI codes.
	  *
	  * @param txt	Input string.
	  * @return		String with any ANSI codes removed.
	  */
	def ansiStrip (txt: String): String = {
		var output = txt
		var pos = output.indexOf("\u001b[")
		var end = pos + 2
		var tmp = ""
		while (pos >= 0) {
			while ((end < output.length - 1) && ("01234567890;".contains(output.substring(end, end + 1)))) {
				end += 1
			}
			tmp = ""
			if (pos > 0)
				tmp = output.substring(0, pos)
			if (end < output.length - 1)
				tmp += output.substring(end + 1, output.length)
			output = tmp
			pos = output.indexOf("\u001b[")
			end = pos + 2
		}
		output
	}

	/**
	  * Active ANSI code object.
	  *
	  * @param codeType
	  * @param codeStr
	  */
	private class ANSIActive(codeType: String, codeStr: String) {
		var ctype = codeType
		var code = codeStr
	}
	/**
	  * Returns the active ANSI style and color codes up to and including the Nth visible character in a string.
	  *
	  * @param txt		The string to search within.
	  * @param charPos	The Nth visible character position.
	  * @return			The ANSI codes which apply at that position.
	  */
	def ansiGetActiveCodesAtPos(txt: String, charPos: Int): String = {
		var activeCodes = ArrayBuffer[ANSIActive]()
		var codeMatch = Option.empty[ANSIElement]
		var output = txt
		var code = ""
		var pos = output.indexOf("\u001b[")
		var end = pos + 2
		var tmp = ""
		var idx = 0
		while (pos >= 0 && pos <= charPos) {
			while ((end < output.length - 1) && ("01234567890;".contains(output.substring(end, end + 1)))) {
				end += 1
			}
			tmp = ""
			if (pos > 0)
				tmp = output.substring(0, pos)
			if (end < output.length - 1)
				tmp += output.substring(end + 1, output.length)
			code = output.substring(pos + 2, end + 1)
			codeMatch = ansiTypesList.find(x => code.matches(x.code))  // Look for a matching style or color code
			if (codeMatch != None) {  // Activate/deactivate matching code type
				idx = activeCodes.indexWhere(_.ctype == codeMatch.get.ctype)
				if (idx < 0) {
					if (!codeMatch.get.disable) {
						activeCodes += new ANSIActive(codeMatch.get.ctype, code)  // Add new active code
					}
				} else {
					if (codeMatch.get.disable) {
						activeCodes.remove(idx)  // Remove disabled code
					} else {
						activeCodes(idx).code = code  // Update changed code
					}
				}
			} else {
			}
			output = tmp
			pos = output.indexOf("\u001b[")
			end = pos + 2
		}
		var ret = ""
		activeCodes.foreach(x => ret += "\u001b[" + x.code)
		ret
	}

	/**
	  * Gets the portion of the string starting from the visible starting character position `beginIndex` to just before `endIndex`.
	  * - Note: This assumes that this is a single horizontal line of characters, with no cursor repositioning.
	  *
	  * @param sourceString	Original string.
	  * @param beginIndex	Starting visible character index, inclusive.
	  * @param endIndex		Ending visible character index, exclusive.  Use `-1` to include the rest of the string.  (default = `-1`)
	  * @return				Substring, including appropriate ANSI codes.
	  */
	def ansiSubstring (sourceString: String, beginIndex: Int, endIndex: Int = -1): String = {
		var output = sourceString
		var srclen = ansiStrip(output).length
		var firstPos = beginIndex.max(0)
		var finalPos = endIndex
		if ((finalPos < 0) || (finalPos > srclen))  // Prevent searching past the end and use negative numbers to search to include the end of the string
			finalPos = srclen
		var ret = ""
		if (firstPos <= finalPos) {  // Get substring
			var pos = output.indexOf("\u001b[")
			var end = pos + 2
			var prvPos = 0
			var lastCodes = ""
			var tmp = ""
			while ((pos >= 0) && (pos < finalPos)) {
				while ((end < output.length - 1) && ("01234567890;".contains(output.substring(end, end + 1))))  // Find end of ANSI code
					end += 1
				tmp = ""
				if (pos > 0)  // Get all text prior to this ANSI code
					tmp = output.substring(0, pos)
				if (end < output.length - 1)  // Add the remaining string after this ANSI code
					tmp += output.substring(end + 1, output.length)
				if ((prvPos < pos) && (lastCodes != "") && (ret == "")) {  // Add previous ANSI codes if we've already started building a substring
					ret += lastCodes
					lastCodes = ""
				}
				if ((pos >= firstPos) && (ret == "")) {  // Grab start of substring
					if (pos > 0) {
						ret = ansiGetActiveCodesAtPos(sourceString, firstPos) + output.substring(firstPos, pos)
						lastCodes = ""
					}
				} else if (pos >= firstPos) {
					if (pos < finalPos) {  // Grab middle of substring
						ret += lastCodes + output.substring(prvPos, pos)
					} else if (ret == "") {  // Grab end of substring
						ret = ansiGetActiveCodesAtPos(sourceString, firstPos) + output.substring(prvPos, output.length)
					} else {  // Grab the remainder of the substring
						ret += output.substring(prvPos, finalPos)
					}
				}
				if (ret != "")
					lastCodes += output.substring(pos, end + 1)
				output = tmp
				prvPos = pos
				pos = output.indexOf("\u001b[")
				end = pos + 2
			}
			if (ret == "")
				ret = ansiGetActiveCodesAtPos(sourceString, firstPos) + output.substring(firstPos, finalPos)
			else
				ret += lastCodes + output.substring(prvPos, finalPos)
		}
		ret
	}

	/**
	  * Replaces characters in a source string with another string, starting from a visible character position.
	  * Attempts to maintain ANSI codes to the right of the inserted section.
	  * - Note: This assumes that this is a single horizontal line of characters, with no cursor repositioning.
	  *
	  * @param sourceString	The original string.
	  * @param insertString	The string that will replace part of the original string.
	  * @param insertPos	The visible character position within that string to start the replacement.
	  * @return				The modified string, which will be the same width as the original string.
	  */
	def ansiReplaceString (sourceString: String, insertString: String, insertPos: Int): String = {
		var result = ""
		val srcLen = ansiStrip(sourceString).length
		var insertStr = insertString
		var insertLen = ansiStrip(insertStr).length
		var insertStart = insertPos
		if (insertStart < 0) {
			insertStr = ansiSubstring(insertStr, -insertStart)
			insertLen = ansiStrip(insertStr).length
			insertStart = 0
		}
		if ((insertLen > 0) && (srcLen > insertStart)) {
			if (insertStart == 0)  // Insert at beginning
				result = insertStr + ansiReset + ansiSubstring(sourceString, insertLen)
			else if (insertStart + insertLen < srcLen)  // Insert in the middle
				result = ansiSubstring(sourceString, 0, insertStart) + ansiReset + insertStr + ansiReset + ansiSubstring(sourceString, insertStart + insertLen)
			else {  // Insert at the end
				if (insertStart + insertLen > srcLen) {
					insertStr = ansiSubstring(insertStr, 0, srcLen - insertStart)
					insertLen = insertStr.length
				}
				result = ansiSubstring(sourceString, 0, insertStart) + ansiReset + insertStr
			}
		} else  // insertString is not within sourceString
			result = sourceString
		result
	}


	// Extend ANSICodes methods to the String class:

	implicit class StringImprovements(val s: String) {
		/**
		  * Strips the string of all ANSI codes, including ones not otherwise supported by ANSICodes.
		  *
		  * @return	The string bereft of ANSI codes.
		  */
		def ansiStrip = ANSICodes.ansiStrip(s)

		/**
		  * Returns the active ANSI style and color codes up to and including the Nth visible character in a string.
		  *
		  * @param charPos	The Nth visible character position.
		  * @return			The ANSI codes which apply at that position.
		  */
		def ansiGetActiveCodesAtPos(charPos: Int) = ANSICodes.ansiGetActiveCodesAtPos(s, charPos)

		/**
		  * Gets the portion of the string starting from the visible starting character position `beginIndex` to just before `endIndex`.
		  * - Note: This assumes that this is a single horizontal line of characters, with no cursor repositioning.
		  *
		  * @param	beginIndex	Starting visible character index, inclusive.
		  * @param	endIndex	Ending visible character index, exclusive.  Use `-1` to include the rest of the string.  (default = `-1`)
		  * @return				Substring, including appropriate ANSI codes.
		  */
		def ansiSubstring (beginIndex: Int, endIndex: Int) = ANSICodes.ansiSubstring(s, beginIndex, endIndex)

		/**
		  * Replaces characters in a source string with another string, starting from a visible character position.
		  * Attempts to maintain ANSI codes to the right of the inserted section.
		  * - Note: This assumes that this is a single horizontal line of characters, with no cursor repositioning.
		  *
		  * @param insertString	The string that will replace part of the original string.
		  * @param insertPos	The visible character position within that string to start the replacement.
		  * @return				The modified string, which will be the same width as the original string.
		  */
		def ansiReplaceString (insertString: String, insertPos: Int) = ANSICodes.ansiReplaceString(s, insertString, insertPos)
	}
}