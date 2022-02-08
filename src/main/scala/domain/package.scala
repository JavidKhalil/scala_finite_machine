package object domain {

  abstract sealed class State(value: String){
    def currentState = value
    def previosState: Option[State]
    def nextState: Option[State]
  }
  case object Init extends State("init") {
    override def previosState: Option[State] = None

    override def nextState: Option[State] = Some(Pending)
  }
  case object Pending extends State("pending") {
    override def previosState: Option[State] = Some(Init)

    override def nextState: Option[State] = Some(Finished)
  }
  case object Finished extends State("finished") {
    override def previosState: Option[State] = Some(Pending)

    override def nextState: Option[State] = None
  }

  object State {
    private def states = Set("init", "pending", "finished")

    def generateStateFromInput(value: String): State = {
      states.find(_ == value) match {
        case Some(value) => State.generateStateFromInput(value)
        case None => State.generateStateFromInput("unknown")
      }
    }
  }

  case class EntityDomain(id: Option[Long], name: String = "no name", currentState: String = "init"){
    def prev = State.generateStateFromInput(currentState).previosState
    def next = State.generateStateFromInput(currentState).nextState

    override def toString: String = s"${id.get} $name $currentState"
  }

  case object EntityDomainNotFound
}
