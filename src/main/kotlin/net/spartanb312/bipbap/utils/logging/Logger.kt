package net.spartanb312.bipbap.utils.logging

object Logger : ILogger by SimpleLogger(
    "Bipbap",
    //"logs/${SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Date())}.txt"
)