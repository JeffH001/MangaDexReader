# MangaDexReader
## Table of Contents

* <a href="#project-description">Project Description</a>
* <a href="#made-with">Made With...</a>
* <a href="#the-ui">The UI</a>

## Project Description
A test project using Scala, Spark, and Hive to read data from the MangaDex API using a console application.

This project pulls data from the [MangaDex](https://mangadex.org/) website using the public [MangaDex API](https://api.mangadex.org/docs/).  It can download information on 20 manga (since this was just for testing) with various search constraints (using the `getMangaData` function) and download the chapter information for a specific manga (using the `getChapters` function).  Both of these functions work without exceeding the MangaDex API's rate limits (due to the `getData` function).  If there are more than two chapters in a manga, you can use the `getMangaStats` function to get information on the average number of pages per chapter and the average time between chapter releases for a manga.

The code also has functions in place to handle users, including creating users (the `login` function), login/logout (the `login` and `logout` function), deleting users (the `userDelete` function), and changing passwords (the `userUpdate` function).  The passwords are stored as hashes in the database (using the `hashStr` function).

I have also been working on a console user interface (UI) in my free time (tenatively called CLITool), so I tried to incorporate the little bit of it that I had working into this project.  (Please note that it is currently very early in development and fragile.)

The plan was to have the ability for users to search for manga, add them to their personal lists, and track new chapter releases, but these features were not added due to lack of time.

## Made With...
- Scala v2.12.14
  - [Requests-Scala](https://github.com/com-lihaoyi/requests-scala) v0.7.0
  - [µPickle](https://github.com/com-lihaoyi/upickle) v0.9.5
  - [nscala-time](https://github.com/nscala-time/nscala-time) v2.30.0
  - [JLine](https://github.com/jline/jline3) v3.21.0
- sbt v1.6.2
- Java v8 (v1.8.0_312)
- Spark v3.1.3
- Hive v3.1.2
- Hadoop v3.3.0
- VSCode v1.66.1
  - Scala (Metals) extension (by Scalameta) v1.13.0
  - Remote Development extension (by Microsoft) v0.21.0
- Ubuntu v20.04 LTS

## The UI

<div align="center"><img alt="MangaDexReader welcome screen" src="/images/Message1.png?raw=true" height=300></div>

When the code is run from sbt in the VSCode terminal window, after the code finishes initializing you'll be greeted with the above welcome screen.

Once you hit a key to continue, you can then hit the `INSERT` key to bring up the main menu:

<div align="center"><img alt="MangaDexReader welcome screen" src="/images/Menu1.png?raw=true" height=300></div>

If you select "**Login**", you will then be able to log in as a user via the login window:

<div align="center"><img alt="MangaDexReader welcome screen" src="/images/Login1.png?raw=true" height=300></div>

On the first run, if the username is "admin", then the password you enter will be set as the default password for the admin account.

For any other username, if the username doesn't exist in the database, then it will add them as a user.

If the username does exist and it has a password, then the login will only work if the password that was entered exactly matches what was entered previously.

Choosing "**Log Out**" from the menu will let you log out.

Choosing "**Quit**" will quit the application.

Unfortunately, I didn't have time to include accessing the manga queries from the UI, but if you choose "**Quit + Run Queries**" then it will close the UI and show the output of some example queries, including running the `getMangaStats` function on two manga to generate statistics on them if they have more than two chapters.
