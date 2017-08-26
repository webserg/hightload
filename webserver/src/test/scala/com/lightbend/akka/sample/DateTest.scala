package com.lightbend.akka.sample

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.{LocalDate, Period}

object DateTest extends App{

  private def getAge(bd: Long) = {
    import java.time.LocalDate
    val today: LocalDate = LocalDate.now
    var res = Period.between(LocalDate.ofEpochDay(bd), today).getYears
    res
  }
  import java.time.LocalDate
  val today = LocalDate.now
  println(getAge(902620800))
  println(LocalDate.ofEpochDay(today.toEpochDay - 902620800))
  println(LocalDate.ofEpochDay(today.toEpochDay).getYear)
  println(today.getYear - LocalDate.ofEpochDay(902620800 / (24 * 60*60)).getYear)
  println(LocalDate.of(1998,8,9).toEpochDay / 365)
  println(LocalDate.of(1998,8,9).toEpochDay * 24 * 60 * 60)
  println(LocalDate.ofEpochDay(365).getYear)
  val df = new SimpleDateFormat("yyyy-MM-dd")
  val date = df.format(902620800 )
  println(date)

  "GET\t/users/146/visits?toDistance=18&toDate=1540771200&fromDate=1386720000\t200\t{\"visits\": []}".split("\t").foreach(println)

  case class Plane(name:String)

  var jetSet = Set(Plane("Boeing"), Plane("Airbus"))
  jetSet += Plane("Lear")
  jetSet += Plane("AirBus")
  println(jetSet.contains(Plane("Cessna")))
  println(None.nonEmpty)
  println(None.isDefined)
  println(Some("fsdsd").nonEmpty)


  def llist() = List(1,2,3)

  val res = for(l <- List(1,2,3)) yield llist()
  println(res.flatten)
}

