package test

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.FunSuite
import ru.mfms.config.TsConfig

class TestSuit extends FunSuite with TsConfig with LazyLogging{


    test("connection string") {
        val connectString = config.getString("app.zoo.connectString").split(",").map {
            item ⇒
                val items = item.split(":")
                if (items.length == 2) {
                    val port = try {
                        items(1).toInt
                    } catch {
                        case e: Throwable ⇒
                            logger error (e.getMessage, e)
                            2181
                    }
                    Some(items(0).trim -> port)
                }
                else if (items.length == 1)
                    Some(items.head.trim -> 2181)
                else
                    None
        }.filter(_.isDefined).map(_.get)

        println(s"${connectString.mkString("[", ", ", "]")}")
    }
}
