
import scala.util.parsing.json.JSON

class MonsterModel(name: String, spells: List[String]) {
    override def toString : String = "{ name: " + name + ", spells: " + spells + " }"
}

class CastExtractor[T] {
    def unapply(a:Any): Option[T] = Some(a.asInstanceOf[T])
}

object Main extends App {
    val jFile = scala.io.Source.fromFile("../monsters.json")
    val jString = try jFile.mkString finally jFile.close()
    val Some(json) = JSON.parseFull(jString)

    val exList    = new CastExtractor[List[Any]]
    val exMap     = new CastExtractor[Map[String, Any]]
    val exStr     = new CastExtractor[String]
    val exStrList = new CastExtractor[List[String]]

    val monsters = for {
        // 'json' is wrapped into a list to allow for expression
        exList(monsters) <- List(json)
        exMap(monster)   <- monsters
        exStr(name)       = monster("name")
        exStrList(spells) = monster("spells")
    } yield {
        new MonsterModel(name, spells)
    }
    monsters.foreach(println(_))
}
