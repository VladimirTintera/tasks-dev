package eu.tintera.tasks

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.*
import kotlin.time.Clock

fun xx(message: String) {

}

fun log(message: String) {
    val fileManager = NSFileManager.defaultManager
    val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val docDir = urls.first() as NSURL
    val fileUrl = docDir.URLByAppendingPathComponent("background_debug.txt")!!
    val path = fileUrl.path!!
    val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())


    val logLine = "[$timestamp]: $message\n"
    val data = (logLine as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!

    if (!fileManager.fileExistsAtPath(path)) {
        data.writeToFile(path, true)
    } else {
        val fileHandle = NSFileHandle.fileHandleForWritingAtPath(path)
        fileHandle?.seekToEndOfFile()
        fileHandle?.writeData(data)
        fileHandle?.closeFile()
    }
}