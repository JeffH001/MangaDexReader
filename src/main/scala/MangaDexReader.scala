package com.JeffH001

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer
import org.apache.log4j.{ Level, Logger }
import org.apache.spark.sql.{ SparkSession, SaveMode, Row, DataFrame }
import org.apache.spark.sql.types._  // { StringType, StructField, StructType, DataFrame }
import com.github.nscala_time.time.Imports._
import java.sql.Timestamp
import java.sql.Date
import requests.Response
import com.JeffH001.ANSICodes._
import com.JeffH001.CLITool._

object MangaDexReader {

	// Variables:

	/**
	  * Tracks the last 5 times that data has been gotten from the MangaDex servers to avoid exceeding the rate limit of 5 gets/second.
	  */
	private var dataGetTimes = Array.fill[DateTime](5)(DateTime.lastYear())
	/**
	  * User mode: `-1` = not logged in, `0` = admin, `1`= normal user
	  *
	  * @type Int
	  */
	private var usermode = -1
	private var currentUser = ""
	private var spark:SparkSession = null


	// Methods:

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
	  * Gets data from the web at a rate no greater than 5 times per second.
	  *
	  * @param url	URL to get the data from.
	  * @return		A Response object with the site data.
	  */
	private def getData(url: String): Response = {
		while (dataGetTimes(0) + 1.second > DateTime.now())  // Wait a bit to prevent exceeding rate limits
			Thread.sleep(200)
		for (i <- 0 to 3)  // Update get time tracking with new get time
			dataGetTimes(i) = dataGetTimes(i + 1)
		dataGetTimes(4) = DateTime.now()
		requests.get(url)  // Get the data
	}

	/**
	  * Turns a string into an integer hash.
	  *
	  * @param password	String to be hashed.
	  * @return			Resulting hash integer (empty strings return 0).
	  */
	def hashStr (password: String): Int = {
		var hash = 0
		if (password.length > 0)
			for (i <- password) {
				hash  = ((hash << 5) - hash) + i.toInt
				hash |= 0  // Convert to 32bit integer
			}
		hash
	}

	/**
	  * Updates the PassHash and Class values where User = username in the "users" table.
	  *
	  * @param username		Username.
	  * @param userClass	User class (`0` = admin, `1` = normal user).
	  * @param passHash		Hashed password.
	  */
	private def userUpdate (username: String, userClass: Int, passHash: Int): Unit = {
		var userTable = spark.sql("SELECT * FROM users")
		if (userTable.where(s"Username = '$username'").count() > 0) {
			var updatedTable = Seq.empty[Row]
			for (r <- userTable.rdd.collect()) {  // Copy existing data but change the hash for this user
				if (r.getString(0) == username)
					updatedTable = updatedTable :+ Row(username, userClass, passHash)  // Update row
				else
					updatedTable = updatedTable :+ Row(r.getString(0), r.getInt(1), r.getInt(2))  // Copy row
			}
			val tableStructure = new StructType()  // Describe the data
				.add("Username", StringType, false)
				.add("Class", IntegerType, false)
				.add("PassHash", IntegerType, false)
			val df = spark.createDataFrame(spark.sparkContext.parallelize(updatedTable), tableStructure)  // Read the data into the dataframe
			df.createOrReplaceTempView("temptable")  // Registers the dataframe as "temptable"
			spark.sql("INSERT OVERWRITE TABLE users SELECT * FROM temptable")  // Reloads the data in the table from the dataframe
			spark.catalog.dropTempView("temptable")  // No longer needed
			println(s"Udated user: $username")
		} else {
			println(s"Could not find user '$username' to update.")
		}
	}

	/**
	  * Deletes entries where User = username from the "users" table.
	  *
	  * @param username	User to delete.
	  */
	private def userDelete (username: String): Unit = {
		var userTable = spark.sql("SELECT * FROM users")
		if (userTable.where(s"Username = '$username'").count() > 0) {
			var updatedTable = Seq.empty[Row]
			for (r <- userTable.rdd.collect()) {  // Copy existing data but change the hash for this user
				if (r.getString(0) != username)
					updatedTable = updatedTable :+ Row(r.getString(0), r.getInt(1), r.getInt(2))  // Copy row
			}
			val tableStructure = new StructType()  // Describe the data
				.add("Username", StringType, false)
				.add("Class", IntegerType, false)
				.add("PassHash", IntegerType, false)
			val df = spark.createDataFrame(spark.sparkContext.parallelize(updatedTable), tableStructure)  // Read the data into the dataframe
			df.createOrReplaceTempView("temptable")  // Registers the dataframe as "temptable"
			spark.sql("INSERT OVERWRITE TABLE users SELECT * FROM temptable")  // Reloads the data in the table from the dataframe
			spark.catalog.dropTempView("temptable")  // No longer needed
			println(s"Deleted user: $username")
			// ToDo: Delete user data from other tables
		} else {
			println(s"Could not find user '$username' to delete.")
		}
	}

	/**
	  * Attempts to log into the database using the given username and password.
	  * - Unknown usernames will be added to the user list as normal users.
	  * - Logins with a password hash of 0 will set the user's password hash.
	  * - Logins which don't match a non-zero password hash will fail.
	  *
	  * @param username	The user's username.
	  * @param password The password.
	  * @return			Whether a user was added/updated/logged in or if the login failed.
	  */
	private def login (username: String, password: String): Boolean = {
		//var userTable = spark.sql(s"SELECT * FROM users WHERE Username = '$username'")
		var userTable = spark.sql("SELECT * FROM users")
		var userData = userTable.where(s"Username = '$username'")
		if (userData.count() > 0) {  // Check existing user for password hash match
			var uclass = userData.first.getInt(1)
			var phash = userData.first.getInt(2)
			val hashVal = hashStr(password)
			println(s"Class: $uclass  /  Password hash: $phash")
			println(s"Password: $password  /  Hash: $hashVal")
			if (phash == 0) {  // Add a new password
				userUpdate(username, uclass, hashVal)
				usermode = uclass
				currentUser = username
				println(s"Added new password to user '$username'.  (mode = $usermode)")
				println("\nUpdated users table:")
				spark.sql("SELECT * FROM users").show(false)
				true
			} else if (phash == hashVal) {  // Password hash matches
				usermode = uclass
				currentUser = username
				println(s"User '$username' successfully logged in.  (mode = $usermode)")
				true
			} else {  // Password hash doesn't match
				println(s"Password did not match for user '$username'.")
				false
			}
		} else {  // Create a new user as a "normal user"
			var hashVal = 0
			if (password != "")
				hashVal = hashStr(password)
			spark.sql(s"INSERT INTO users (Username, Class, PassHash) VALUES ('$username', 1, $hashVal)")
			println(s"Created new user: $username")
			true
		}
	}

	private def logout (): Unit = {
		usermode = -1
		currentUser = ""
		println("Logged out.\n")
	}

	/**
	  * Attempts to get manga data which matches the given parameters and returns it as an Option[Seq[Row]]
	  * - Limited to 100 results per search, use the offset to get more.
	  * - NOTE: This demo is currently limited to "safe" manga with English titles.
	  *
	  * @param title	Title to search for.  (default = `""`)
	  * @param year		Year to search in.  (default = `0`)
	  * @param status	Array of acceptable manga statuses.  (default = `Array("ongoing", "completed", "hiatus", "cancelled")`)
	  * @param offset	Offset into the search.  (default = `0`)
	  * @return			Returns `None` or data as `Option[Seq[Row]]` of MangaID, Title, Language, Status, Year
	  */
	private def getMangaData (title: String = "", year: Int = 0, status: Array[String] = Array("ongoing", "completed", "hiatus", "cancelled"), offset: Int = 0): Option[Seq[Row]] = {
		var params = "availableTranslatedLanguage[]=en&contentRating[]=safe&limit=20"  // *** Limiting to 20 instead of 100 for testing purposes
		if (offset != 0)
			params += "&offset=" + offset
		if (year != 0)
			params += "&year=" + year
		for (s <- status)
			params += "&status[]=" + s
		if (title != "")
			params += "&title=" + title
		var webdat = getData("https://api.mangadex.org/manga?" + params)
		if (webdat.statusCode != 200) {  // Failed to get anything
			println("Error: Unable to download data (status = " + webdat.statusCode + ")")
			None
		} else {
			val jsondat = ujson.read(webdat.text)
			var count = jsondat("total").num.toInt
			if (count > jsondat("limit").num.toInt)
				count = jsondat("limit").num.toInt
			var datob = jsondat("data")(0)("attributes")
			var title = datob("title").obj.get("en")
			var tableData = Seq.empty[Row]
			var id = ""
			var t = ""
			var lang = ""
			var state = ""
			var year = 0

			// Read the JSON data into a Seq of Row elements
			for(i <- 0 to count - 1) {
				datob = jsondat("data")(i)
				title = datob("attributes")("title").obj.get("en")
				if (title != None) {  // Has an English title
					//t = "'" + sanitizeString(title.get.str) + "'"
					t = sanitizeString(datob("attributes")("title")("en").str)
					id = sanitizeString(datob("id").str)
					lang = sanitizeString(datob("attributes")("originalLanguage").str)
					state = sanitizeString(datob("attributes")("status").str)
					if (datob("attributes")("year").isNull)
						year = 0
					else
						year = datob("attributes")("year").num.toInt
					tableData = tableData :+ Row(id, t, state, year, lang)  // MangaID, Title, Status, Year, Language
					/*
					println(i + ": MangaID = " + id)
					println("Title = " + t) //tableData.last.getString(1))
					println("Status = " + state)
					println("Year = " + year)
					println("Language = " + lang)
					*/
				}
			}
			Option(tableData)
		}
	}

	/**
	  * Reads chapter data for a manga into the "chapters" database if it's not already there.
	  *
	  * @param mangaID	ID of the manga to get.
	  * @return			The Option[DataFrame] of the resulting data.
	  */
	private def getChapters(mangaID: String): Option[DataFrame] = {
		var userTable = spark.sql(s"SELECT * FROM chapters WHERE MangaID = '$mangaID'")
		if (userTable.count() == 0) {
			val params = s"https://api.mangadex.org/chapter?manga=${mangaID}&translatedLanguage[]=en&limit=100"
			var webdat = getData(params)
			if (webdat.statusCode != 200) {  // Failed to get anything
				println("Error: Unable to download data (status = " + webdat.statusCode + ")")
				None
			} else {
				var tableData = Seq.empty[Row]
				val jsondat = ujson.read(webdat.text)
				var offset = jsondat("offset").num.toInt
				var count = jsondat("total").num.toInt - offset
				if (count > jsondat("limit").num.toInt)
					count = jsondat("limit").num.toInt
				if (count > 0) {  // There's data to be read
					var datob = jsondat("data")(0)
					var title = ""
					var vol = ""
					var chap = ""
					var createdAt = ""
					var pages = 0
					// Read the JSON data into a Seq of Row elements
					println("MangaID = " + mangaID)
					for(i <- 0 to count - 1) {
						datob = jsondat("data")(i)("attributes")
						if (datob("title").isNull)
							title = ""
						else
							title = sanitizeString(datob("title").str)
						if (datob("volume").isNull)
							vol = ""
						else
							vol = sanitizeString(datob("volume").str)
						if (datob("chapter").isNull)
							chap = ""
						else
							chap = sanitizeString(datob("chapter").str)
						createdAt = sanitizeString(datob("createdAt").str)
						pages = datob("pages").num.toInt
						tableData = tableData :+ Row(mangaID, title, vol, chap, createdAt, pages)  // MangaID, Title, Volume, Chapter, CreatedAt, Pages
						/*
						println(s"$i: Chapter = '$chap'  /  Volume = '$vol'")
						println(s"Title = '$title'")
						println(s"CreatedAt = '$createdAt'")
						println(s"Pages = $pages")
						*/
					}
					// *** Read more?  webdat = getData(params + "&offset=" + offset)
					// Build dataframe
					val tableStructure = new StructType()  // Describe the data
						.add("MangaID", StringType, false)
						.add("ChTitle", StringType, false)
						.add("Volume", StringType, false)
						.add("Chapter", StringType, false)
						.add("CreatedAt", StringType, false)
						.add("Pages", IntegerType, false)
					val df = spark.createDataFrame(spark.sparkContext.parallelize(tableData), tableStructure)  // Read the data into the dataframe
					Option(df)
				} else {  // No chapter data available for this manga.
					println("No chapter data available for this manga.")
					None
				}
			}
		} else {  // Chapters for this manga are already in the table.
			println("Chapters for this manga are already in the table.")
			None
		}
	}

	/**
	  * Get various stats about manga chapters.
	  *
	  * @param mangaID	MangaID to run stats on.
	  */
	private def getMangaStats (mangaID: String): Unit = {
		var userTable = spark.sql(s"SELECT * FROM chapters WHERE MangaID = '$mangaID' ORDER BY CreatedAt")
		userTable.show(false)
		if (userTable.count() > 1) {
			var count = 0
			var totalPages = 0
			val format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")  // Example time: 2022-03-28T00:46:56+00:00
			var lastDate = 0L
			var newDate = 0L
			var totalTime = 0L
			for (r <- userTable.rdd.collect()) {  // Copy existing data but change the hash for this user
				count += 1
				newDate = format.parse(r.getString(4)).getTime() / 1000
				if (lastDate > 0)
					totalTime += newDate - lastDate
				lastDate = newDate
				totalPages += r.getInt(5)  // Pages
			}
			println("Average pages per chapter: " + (totalPages.toDouble / count))
			var seconds = totalTime / (count - 1)
			var minutes = seconds / 60
			seconds -= minutes * 60
			var hours = minutes / 60
			minutes -= hours * 60
			var days = hours / 24
			hours -= days * 24
			println(s"Average time between chapters: $days days and $hours hours")
		} else {
			println("No stats available for this manga.")
		}
	}

	def main (args: Array[String]): Unit = {

		var term = CLITool.init()
		CLITool.addMenu(Array("File", "Login", "Quit"), false)
		CLITool.addMessage("Loading...", "Please wait while I download the latest manga data...", 35, false)
		CLITool.termUpdate(true)
		// Read data from the web
		var tableData = getMangaData("", 2022)
		if (tableData != None) {

			// Start the Spark session
			System.setProperty("hadoop.home.dir", "C:\\hadoop")  // ToDo: Change this directory if needed.
			Logger.getLogger("org").setLevel(Level.ERROR)  // Hide most of the initial non-error log messages
			//val warehouseLocation = new File("spark-warehouse").getAbsolutePath  // warehouseLocation points to the default location for managed databases and tables
			spark = SparkSession.builder
				.appName("MangaDex")
				//.config("spark.sql.warehouse.dir", warehouseLocation)
				.config("spark.master", "local[*]")
				.enableHiveSupport()
				.getOrCreate()
			spark.sparkContext.setLogLevel("ERROR")  // Hide further non-error messages
			// println("Created Spark session.")
			CLITool.termUpdate(true)

			// Build dataframe
			val tableStructure = new StructType()  // Describe the data
				.add("MangaID", StringType, false)
				.add("Title", StringType, false)
				.add("Status", StringType, false)
				.add("Year", IntegerType, false)
				.add("Language", StringType, false)  // Partitioned by this so it must be last
			val df = spark.createDataFrame(spark.sparkContext.parallelize(tableData.get), tableStructure)  // Read the data into the dataframe
			/*
			println("\nDataframe schema:")
			df.printSchema()
			println("Dataframe data:")
			df.show(100, false)
			*/

			CLITool.deleteWin("Loading...")
			CLITool.addMessage("Loading...", "Please wait while I build the tables...", 35, false)
			CLITool.termUpdate(true)
			// Build database
			spark.sql("SET hive.exec.dynamic.partition.mode=nonstrict")
			spark.sql("CREATE DATABASE IF NOT EXISTS mangadex")
			spark.sql("USE mangadex")

			// Build "users" table
			// spark.sql("DROP TABLE IF EXISTS users")
			if (!spark.catalog.tableExists("users")) {  // Create table with default "admin" account
				spark.sql("CREATE TABLE users (Username STRING, Class INT, PassHash INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE")
				spark.sql("INSERT INTO users (Username, Class, PassHash) VALUES ('admin', 0, 0)")
			}
			//println("Users table:")
			//spark.sql("SELECT * FROM users").show(false)

			CLITool.deleteWin("Loading...")
			CLITool.addMessage("Welcome to the MangaDex Reader.", "You can use the INSERT key to select an item from the menu.\n\n(Hit any key to begin.)", 35)

			var continue = true
			CLITool.termUpdate(true)
			var cmd = new CLITool.Command()
			while (continue) {
				cmd = CLITool.getCommand()
				print(ansiResetLn)
				cmd.raw match {
					case ""						=>  // Do nothing
					case "q" | "quit" | "Quit"	=> continue = false
					case "Login"				=> {  // Handle login
						CLITool.addInput("Login Window", "Login or create a new account.", LinkedHashMap(("Username:", ""), ("~Password:", "")), 35)
						cmd = CLITool.getCommand()
						if (login(cmd.args(0), cmd.args(1))) {
							print(ansiResetLn + s"Logged in as '${cmd.args(0)}'.")
							CLITool.addMenu(Array("File", "Log Out", "Quit"), false)
						} else {
							print(ansiResetLn + s"Unable to log in as '${cmd.args(0)}'.  Incorrect password.")
						}
					}
					case "Log Out"				=> {  // Handle log out
						logout()
						print(ansiResetLn + "Logged out.")
						CLITool.addMenu(Array("File", "Login", "Quit"), false)
					}
					case _ 						=> print(ansiResetLn + s"Unknown command: '${cmd.raw}'")  // Unknown command
				}
			}

			/*
			// Initialize admin account / login
			if (login("admin", "password"))
				println("\nLogged in as 'admin'.\n")
			else
				println("\nLogin as 'admin' failed.\n")

			// Log out
			logout()

			// Create new user account / login
			if (login("newUser", "password"))
				println("\nLogged in as 'newUser'.\n")
			else
				println("\nLogin as 'newUser' failed.\n")
			spark.sql("SELECT * FROM users").show(false)

			// Delete new user
			userDelete("newUser")
			spark.sql("SELECT * FROM users").show(false)

			// Build "manga" table
			spark.sql("DROP TABLE IF EXISTS manga")
			spark.sql("CREATE TABLE manga (MangaID STRING, Title STRING, Status STRING, Year INT) PARTITIONED BY (Language STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE")
			df.createOrReplaceTempView("temptable")  // Registers the dataframe as "temptable"
			spark.sql("INSERT OVERWRITE TABLE manga SELECT * FROM temptable")  // Copies data from the dataframe into an actual table
			spark.catalog.dropTempView("temptable")  // No longer needed

			// Show "manga" table
			println("Table partitioned by language:")
			var mangaTable = spark.sql("SELECT * FROM manga")
			mangaTable.show(100, false)
			mangaTable.explain()  // Describe database setup

			// Build "chapters" table
			spark.sql("DROP TABLE IF EXISTS chapters")
			// spark.sql("CREATE TABLE chapters (MangaID STRING, ChTitle STRING, Volume STRING, Chapter STRING, CreatedAt STRING, Pages INT) CLUSTERED BY (MangaID) INTO 20 BUCKETS ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE")
			spark.sql("CREATE TABLE chapters (MangaID STRING, ChTitle STRING, Volume STRING, Chapter STRING, CreatedAt STRING, Pages INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE")

			// Get some chapter data
			var mangaID = mangaTable.first().getString(0)
			var mangaTitle = mangaTable.first().getString(1)
			var chapData = getChapters(mangaID)
			if (chapData != None) {
				chapData.get.createOrReplaceTempView("temptable")  // Registers the dataframe as "temptable"
				var chapterTable = spark.sql("INSERT INTO TABLE chapters SELECT * FROM temptable")
				spark.catalog.dropTempView("temptable")  // No longer needed
				// println("\nChapters table:")
				chapterTable = spark.sql("SELECT * FROM chapters ORDER BY MangaID, Chapter")
				// chapterTable.show(false)  // Copies data from the dataframe into an actual table
				// chapterTable.explain()  // Describe database setup
				println(s"\nShowing stats for manga '${mangaTitle}'")
				getMangaStats(mangaID)
			}
			mangaID = mangaTable.rdd.collect()(1).getString(0)
			mangaTitle = mangaTable.rdd.collect()(1).getString(1)
			chapData = getChapters(mangaID)
			if (chapData != None) {
				chapData.get.createOrReplaceTempView("temptable")  // Registers the dataframe as "temptable"
				var chapterTable = spark.sql("INSERT INTO TABLE chapters SELECT * FROM temptable")
				spark.catalog.dropTempView("temptable")  // No longer needed
				// println("\nChapters table:")
				chapterTable = spark.sql("SELECT * FROM chapters ORDER BY MangaID, Chapter")
				// chapterTable.show(false)  // Copies data from the dataframe into an actual table
				// chapterTable.explain()  // Describe database setup
				println(s"\nShowing stats for manga '${mangaTitle}'")
				getMangaStats(mangaID)
			}
			*/

			// End Spark session
			spark.stop()
			println("Transactions complete.")
		} else {
			println("Quitting without data.")
		}
	}
}
