# Installation
The installation is quite straightforward. But because this is a starter plugin, this installation guide includes the ways of setting up your server from scratch. Very simple. If you already have a third party server service provider, skip to

## Obtaining the server jar
Basically, you aren't going to setup the server with a click of a button. It isn't that easy! What you are going to do instead is download a java jar file which is basically an executable which is basically your server. It extracts itself into different files and folders you will be requiring later. But, this isn't going to be the **Official Minecraft Server Jar**, Instead it is going to be either a Paper, Spigot, Purpur or Bukkit jar. 
Hence, Choose your platform, download your jar, and put it in a folder: 
[Long Button: SpigotMC - Recommended for most variety of expansion later]("https://getbukkit.org/download/spigot")  [Long Button: Paper - Recommended for servers on low performance]("https://papermc.io/downloads/paper")  [Long Button: Purpur - I honestly don't know whats the upside of getting this]("https://purpurmc.org/download/purpur")  [Long Button: Bukkit - Recommended if you want the native support of plugins (Just like spigot)]("https://getbukkit.org/download/craftbukkit")

## Setting up the server
Once you've got the server jar, go to the directory where you downloaded the .jar file, and execute it. Don't worry, you're not going to see stuff for a while, thats ok, because its just extracting resources. Just wait till the *eula.txt* file pops that. Once that pops up, the javaw will auto-close and now we require changes in that file. That file should have a `eula=false`. Set it to `eula=true` and save the file.
Now, you have to start the server for the first time. Open a terminal / use `cd` to focus the directory in your terminal and run this command:
```
java -Xms2G -Xmx4G -jar server.jar nogui
```