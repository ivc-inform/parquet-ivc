package ru.mfms.zoo

import com.ivcinform.zookeeper.{ACL, ConfigurationZookeeperClient, Node, Path, Persistent, PersistentSequential, Session, StateEvent, ZookeeperClient}
import com.typesafe.scalalogging.LazyLogging
import ru.mfms.config.Zoo

class Barrier(zoo: Zoo, debug: Boolean = false) extends LazyLogging {
    private[this] def _notify(str: String): Unit = {
        try {
            notify()
        } catch {
            case _: IllegalMonitorStateException ⇒
                logger warn s"IllegalMonitorStateException in $str"
            case _: InterruptedException ⇒
                logger warn s"InterruptedException in $str"
            case e: Throwable ⇒
                logger warn s"${e.getMessage} in $str"
        }
    }

    private[this] def _wait(str: String): Unit = {
        try {
            wait()
        } catch {
            case _: IllegalMonitorStateException ⇒
                logger warn s"IllegalMonitorStateException in $str"
            case _: InterruptedException ⇒
                logger warn s"InterruptedException in $str"
            case e: Throwable ⇒
                logger warn s"${e.getMessage} in $str"
        }
    }

    private[this] val config = ConfigurationZookeeperClient(ZookeeperClient.strToInetSocketAddress(zoo.connectString))
      .withWatcher((state: StateEvent, session: Session) ⇒ {
          logger debug s"state: $state"
          logger debug s"session: $session"
          _notify("Barrier Watcher.")
      })
      .withTimeout(zoo.timeout)

    private[this] implicit val zookeeperClient = ZookeeperClient(config)
    private[this] val root = zookeeperClient.syncZookeeper.create(zoo.path, Array(), ACL.AnyoneAll, PersistentSequential)

    private[this] val lockPath = Path(root) resolve "lock"
    private[this] val node = Node(lockPath)

    def open(): Unit = synchronized {
        while (node.exists().isDefined)
            _wait("open method in Barrier.")

        node.create(Array(), ACL.AnyoneAll, Persistent)
        if (debug)
            Thread.sleep(500)
        logger debug s"Barrier open."
        notify()
    }

    def close(): Unit = synchronized {
        while (node.exists().isEmpty)
            _wait("close method in Barrier.")

        node.delete()
        if (debug)
            Thread.sleep(500)

        logger debug s"Barrier close."
        _notify("open method in Barrier.")
    }

    def shudown(): Unit = {
        try {
            zookeeperClient.close()
            logger debug "Barrier shutdown."
        } catch {
            case e: InterruptedException ⇒
                logger warn s"InterruptedException close method in Barrier."
            case e: Throwable ⇒
                logger warn s"${e.getMessage} in close method in Barrier."
        }
    }
}

