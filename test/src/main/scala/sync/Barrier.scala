package sync

import ru.mfms.config.Zoo
import ru.mfms.zoo.Barrier

import scala.concurrent.duration._

//object Barrier {
//    var valueSet = true
//    val sleepValue = 500
//
//    def enter(): Unit = synchronized {
//        while (!valueSet)
//            wait()
//
//        println(s"enter.")
//        valueSet = false
//        Thread.sleep(sleepValue)
//        notify()
//    }
//
//    def leave(): Unit = synchronized {
//        while (valueSet)
//            wait()
//
//        valueSet = true
//        println(s"leave.")
//        Thread.sleep(sleepValue)
//        notify()
//    }
//}
//
class Producer(val q: Barrier) extends Runnable {
    new Thread(this, "Open").start()
    override def run(): Unit = {
        while (true)
            q.open()

    }
}

class Consumer(val q: Barrier) extends Runnable {
    new Thread(this, "Close").start()

    override def run(): Unit = {
        while (true)
            q.close()

        println("Close")
    }
}

object PC extends App {
    val barrier = new Barrier(Zoo(timeout = 6 seconds, connectString = "localhost", path = "/test"), true)
    new Producer(barrier)
    new Consumer(barrier)

    println(" Для остановки нажмите Ctrl-C .")
    sys.addShutdownHook {
                () ⇒
                    println(s"system terminating")
                    barrier.shudown()
                    println(s"system terminated")
            }
}
