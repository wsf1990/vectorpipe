package vectorpipe.osm

import io.dylemma.spac._

import java.time.ZonedDateTime

// --- //

/** A sum type for OSM Elements. All Element types share some common attributes. */
sealed trait Element {
  def data: ElementData
}

object Element {
  implicit val elementMeta: Parser[Any, ElementMeta] = (
    Parser.forMandatoryAttribute("id").map(_.toLong) ~
      Parser.forOptionalAttribute("user").map(_.getOrElse("anonymous")) ~
      Parser.forOptionalAttribute("uid").map(_.getOrElse("anonymous")) ~
      Parser.forMandatoryAttribute("changeset").map(_.toInt) ~
      Parser.forMandatoryAttribute("version").map(_.toInt) ~
      Parser.forMandatoryAttribute("timestamp").map(ZonedDateTime.parse) ~
      Parser.forMandatoryAttribute("visible").map(_.toBoolean)
  ).as(ElementMeta)

  /* <tag k='access' v='permissive' /> */
  implicit val tag: Parser[Any, (String, String)] = (
    Parser.forMandatoryAttribute("k") ~ Parser.forMandatoryAttribute("v")
  ).as({ case (k,v) => (k,v) }) // Hand-holding the typesystem.

  implicit val elementData: Parser[Any, ElementData] = (
    elementMeta ~
      Splitter(* \ "tag").asListOf[(String, String)].map(_.toMap)
  ).as(ElementData)

  /* <node lat='49.5135613' lon='6.0095049' ... > */
  implicit val node: Parser[Any, Node] = (
    Parser.forMandatoryAttribute("lat").map(_.toDouble) ~
      Parser.forMandatoryAttribute("lon").map(_.toDouble) ~
      elementData
  ).as(Node)

  /*
   <way ... >
    <nd ref='3867860331'/>
    ...
   </way>
   */
  implicit val way: Parser[Any, Way] = (
    Splitter(* \ "nd")
      .through(Parser.forMandatoryAttribute("ref").map(_.toLong))
      .parseToList
      .map(_.toVector) ~
      elementData
  ).as(Way)

  /* <member type='way' ref='22902411' role='outer' /> */
  implicit val member: Parser[Any, Member] = (
    Parser.forMandatoryAttribute("type") ~
      Parser.forMandatoryAttribute("ref").map(_.toLong) ~
      Parser.forMandatoryAttribute("role")
  ).as(Member)

  implicit val relation: Parser[Any, Relation] = (
    Splitter(* \ "member").asListOf[Member] ~ elementData
  ).as(Relation)

  /** The master parser.
    *
    * ===Usage===
    * {{{
    * val xml: InputStream = new FileInputStream("somefile.osm")
    *
    * val res: Try[(List[Node], List[Way], List[Relation])] = Element.elements.parse(xml)
    * }}}
    */
  val elements: Parser[Any, (List[Node], List[Way], List[Relation])] = (
    Splitter("osm" \ "node").asListOf[Node] ~
      Splitter("osm" \ "way").asListOf[Way] ~
      Splitter("osm" \ "relation").asListOf[Relation]
  ).as({ case (ns, ws, rs) =>
    (ns, ws, rs)
  })
}

/** Some point in the world, which could represent a location or small object
  *  like a park bench or flagpole.
  */
case class Node(
  lat: Double,
  lon: Double,
  data: ElementData
) extends Element

/** A string of [[Node]]s which could represent a road, or if connected back around
  *  to itself, a building, water body, landmass, etc.
  *
  *  Assumption: A Way has at least two nodes.
  */
case class Way(
  nodes: Vector[Long],  /* Vector for O(1) indexing */
  data: ElementData
) extends Element {
  /** Is it a Polyline, but not an "Area" even if closed? */
  def isLine: Boolean = !isClosed || (!isArea && isHighwayOrBarrier)

  def isClosed: Boolean = if (nodes.isEmpty) false else nodes(0) == nodes.last

  def isArea: Boolean = data.tagMap.get("area").map(_ == "yes").getOrElse(false)

  def isHighwayOrBarrier: Boolean = {
    val tags: Set[String] = data.tagMap.keySet

    tags.contains("highway") || tags.contains("barrier")
  }
}

case class Relation(
  members: Seq[Member],
  data: ElementData
) extends Element {
  /** The IDs of sub-relations that this Relation points to. */
  def subrelations: Seq[Long] = members.filter(_.memType == "relation").map(_.ref)
}

case class Member(
  memType: String, // TODO Use a sum type?
  ref: Long,
  role: String // TODO Use a sum type?
)

case class ElementData(meta: ElementMeta, tagMap: TagMap)

/** All Element types have these attributes in common. */
case class ElementMeta(
  id: Long,
  user: String,
  userId: String,
  changeSet: Int,
  version: Int,
  timestamp: ZonedDateTime,
  visible: Boolean
)
