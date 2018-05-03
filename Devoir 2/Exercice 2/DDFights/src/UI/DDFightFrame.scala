package UI

import java.awt.Color
import java.io._

import scala.swing._
import scala.swing.event._
import common._
import engine.{Actor, ActorModel, Engine}

import scala.concurrent.{ExecutionContext, Future}
import scala.swing.GridBagPanel.{Anchor, Fill}

class DDFightFrame extends MainFrame {

    //Variable pour la logique interne du simulateur
    private var InitialScene = List[ActorSetup]()
    private var channel: Channel[List[Actor]] = _
    private var engine: Engine = _
    private var currentSceneInstance : List[Actor] = _
    private var currentTurn = 0
    private var isRunning = false
    private var isPaused = false
    private val chooser = new FileChooser()
    private var dragging = false
    private var lastPos: Point = _
    private var selectedActor: Actor = _
    private var top: Double = -20
    private var left: Double = -20
    private var bottom: Double = 20
    private var right: Double = 20
    private var selectCadre: ((Double,Double),(Double,Double)) = _

    //Fonction pour la logique interne du simulateur
    def getClickedActor(pos: (Double, Double)): Actor = {
        var res: Actor = null
        if(currentSceneInstance != null){
            for (actor <- currentSceneInstance) {
                val dx = actor.pos._1 - pos._1
                val dy = actor.pos._2 - pos._2
                val distance = Math.sqrt(dx * dx + dy * dy)
                if (distance < ActorModel.from(actor.t).size) res = actor
            }
        }
        else{
            for (actor <- InitialScene) {
                val dx = actor.pos._1 - pos._1
                val dy = actor.pos._2 - pos._2
                val distance = Math.sqrt(dx * dx + dy * dy)
                if (distance < ActorModel.from(actor.actorType).size) res = new Actor(-1,actor.actorType, actor.pos)
            }
        }
        res
    }

    def getClickedActorSetup(pos: (Double, Double)): ActorSetup = {
        var res: ActorSetup = null
        for (actor <- InitialScene) {
            val dx = actor.pos._1 - pos._1
            val dy = actor.pos._2 - pos._2
            val distance = Math.sqrt(dx * dx + dy * dy)
            if (distance < ActorModel.from(actor.actorType).size) res = actor
        }
        res
    }

    def drawActor(g: Graphics2D, pos: (Double, Double), size: Double, lifeRate: Double): Unit ={
        var viewPos1 = FieldToView(pos._1 - size, pos._2 - size)
        var viewPos2 = FieldToView(pos._1 + size, pos._2 + size)
        var viewSize = (viewPos2._1 - viewPos1._1,viewPos2._2 - viewPos1._2)
        g.drawOval(viewPos1._1.toInt, viewPos1._2.toInt, viewSize._1.toInt, viewSize._2.toInt)
        val lifeSize = size*lifeRate
        viewPos1 = FieldToView(pos._1 - lifeSize, pos._2 - lifeSize)
        viewPos2 = FieldToView(pos._1 + lifeSize, pos._2 + lifeSize)
        viewSize = (viewPos2._1 - viewPos1._1,viewPos2._2 - viewPos1._2)
        g.fillOval(viewPos1._1.toInt, viewPos1._2.toInt, viewSize._1.toInt, viewSize._2.toInt)
    }

    def drawSelectedActor(g: Graphics2D, pos: (Double, Double), size: Double): Unit = {
        g.setColor(Color.yellow)

        val viewPos1 = FieldToView(pos._1 - size, pos._2 - size)
        val viewPos2 = FieldToView(pos._1 + size, pos._2 + size)
        val viewSize = (viewPos2._1 - viewPos1._1,viewPos2._2 - viewPos1._2)
        g.fillRect(viewPos1._1.toInt, viewPos1._2.toInt, viewSize._1.toInt, viewSize._2.toInt)
    }

    def ViewToField(point: (Double, Double)): (Double, Double) = {

        val fieldLeft = left
        val fieldTop = top
        val fieldRight = right
        val fieldBottom = bottom
        val fieldHeight = fieldBottom - fieldTop
        val fieldWith = fieldRight - fieldLeft

        val viewLeft = 0
        val viewTop = 0
        val viewRight = Field.size.width
        val viewBottom = Field.size.height
        val viewHeight = viewBottom - viewTop
        val viewWidth = viewRight - viewLeft

        (
            (point._1 - viewLeft) * fieldWith / viewWidth + fieldLeft,
            (point._2 - viewTop) * fieldHeight / viewHeight + fieldTop
        )
    }

    def FieldToView(point: (Double, Double)): (Double, Double) = {
        val fieldLeft = left
        val fieldTop = top
        val fieldRight = right
        val fieldBottom = bottom
        val fieldHeight = fieldBottom - fieldTop
        val fieldWidth = fieldRight - fieldLeft

        val viewLeft = 0
        val viewTop = 0
        val viewRight = Field.size.width
        val viewBottom = Field.size.height
        val viewHeight = viewBottom - viewTop
        val viewWidth = viewRight - viewLeft

        (
            (point._1 - fieldLeft) * viewWidth / fieldWidth + viewLeft,
            (point._2 - fieldTop) * viewHeight / fieldHeight + viewTop
        )
    }

    def updateView(): Unit = {
        Field.repaint()
        TurnValue.text = currentTurn.toString
        NameLabel.text = if(selectedActor != null) "Name : " + selectedActor.id + " " + selectedActor.t.toString else "Name : "
        HealthLabel.text = if(selectedActor != null) "Health : " +  selectedActor.life.current + "/" + selectedActor.life.max else "Health : "
        PositionLabel.text = if(selectedActor != null) "Pos : " +  selectedActor.pos else "Pos : "
        actionListModel.setActor(selectedActor)
        actionLogModel.setActor(selectedActor)
    }

    def loadScene(file: File): Unit = {
        val ois = new ObjectInputStream(new FileInputStream(file.getPath))
        val data: SceneSetup = ois.readObject.asInstanceOf[SceneSetup]
        InitialScene = data.actorList
        top = data.top
        left = data.left
        bottom = data.bottom
        right = data.right
        ois.close()
    }

    def saveScene(file: File): Unit = {
        val data = new SceneSetup(InitialScene, top, left, bottom, right)
        val oos = new ObjectOutputStream(new FileOutputStream(file.getPath))
        oos.writeObject(data)
        oos.close()
    }

    def startSimulation(): Unit = {
        channel = new Channel[List[Actor]]
        engine = new Engine(channel, InitialScene)
        engine.start()
        Future{
            while(isRunning){
                if(!isPaused){
                    if(loadNextScene()) Thread.sleep(1000/SpeedSlider.value)
                }
            }
            engine.stop()
            channel = null
            engine = null
        }(ExecutionContext.global)
    }

    def loadNextScene(): Boolean = {
        channel.pop() match{
            case Some(s) =>
                currentTurn += 1
                currentSceneInstance = s
                selectedActor = null
                updateView()
                true

            case None =>
                false
        }
    }

    // Mise en place de l'interface
    import javax.swing.UIManager
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    title = "DDFight Simulation"

    private val CreateButton = new Button("Create New Scene")
    private val LoadButton = new Button("Load Scene")
    private val SaveButton = new Button("Save Scene")
    private val SpeedValue = new Label("1")
    private val TurnValue = new Label("0")
    private val SpeedSlider = new Slider{
        min = 1
        max = 120
        value = 1
    }
    private val StopButton = new Button("Stop")
    private val PlayPauseButton = new Button("Play/Pause")
    private val NextButton = new Button("Next")
    private val actionListModel = new ActionListModel
    private val ActionListTable  = new Table{
        model = actionListModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val actionLogModel = new ActionLogModel
    private val ActionLogTable = new Table{
        model = actionLogModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val monsterListModel = new MonsterListModel
    private val MonsterListTable = new Table {
        model = monsterListModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val NameLabel = new Label("Name : ")
    private val HealthLabel = new Label("Health : ")
    private val PositionLabel = new Label("Pos : ")
    private val Field = new Panel{
        override protected def paintComponent(g: Graphics2D): Unit = {
            //clean view
            g.setColor(Color.white)
            g.fillRect(0, 0, size.width, size.height)

            //draw mobs

            if(selectedActor !=null) {
                val model = ActorModel.from(selectedActor.t)
                if (model.side == ActorSide.Angels) g.setColor(Color.blue)
                else g.setColor(Color.red)
                drawSelectedActor(g, (selectedActor.pos._1, selectedActor.pos._2), model.size)
            }

            if(currentSceneInstance == null && InitialScene.nonEmpty){
                for(actor <- InitialScene){
                    val model = ActorModel.from(actor.actorType)
                    if(model.side == ActorSide.Angels) g.setColor(Color.blue)
                    else g.setColor(Color.red)
                    drawActor(g, (actor.pos._1, actor.pos._2), model.size, 1)
                }
            }
            else if(currentSceneInstance != null){
                for(actor <- currentSceneInstance){
                    val model = ActorModel.from(actor.t)
                    if(model.side == ActorSide.Angels) g.setColor(Color.blue)
                    else g.setColor(Color.red)
                    drawActor(g, (actor.pos._1, actor.pos._2), model.size, actor.life.current / actor.life.max)
                }
            }
            //draw preview
            if(selectCadre != null){

                val actor = ActorType.values.toList(MonsterListTable.selection.rows.leadIndex)
                val actorSize = ActorModel.from(actor).size
                val actorSide = ActorModel.from(actor).side
                var point1 = ViewToField(
                    Math.min(selectCadre._1._1, selectCadre._2._1),
                    Math.min(selectCadre._1._2, selectCadre._2._2)
                )
                var point2 = ViewToField(
                    Math.max(selectCadre._1._1, selectCadre._2._1),
                    Math.max(selectCadre._1._2, selectCadre._2._2)
                )
                val actorMinMarge = 0.25 * actorSize
                val actorMinDistance = 2 * (actorSize + actorMinMarge)

                val xActorCount = ((point2._1 - point1._1)/actorMinDistance).toInt
                val yActorCount = ((point2._2 - point1._2)/actorMinDistance).toInt

                val xActorDistance =  (point2._1 - point1._1)/xActorCount
                val yActorDistance =  (point2._2 - point1._2)/yActorCount

                point1 = (point1._1 + xActorDistance/2, point1._2 + yActorDistance/2)
                point2 = (point2._1 - xActorDistance/2, point2._2 - yActorDistance/2)

                for(i <- 0 until xActorCount){
                    for(j <- 0 until yActorCount){
                        val pos = (point1._1 + i * xActorDistance, point1._2 + j * yActorDistance)
                        if(actorSide == ActorSide.Angels) g.setColor(Color.blue) else g.setColor(Color.red)
                        drawActor(g, pos, actorSize, 1)
                    }
                }
            }
        }
    }
    private val Menu = new GridBagPanel(){
        val c = new Constraints

        //SceneManager
        c.fill = Fill.None
        c.weightx = 0.05
        c.weighty = 0
        c.grid = (0, 0)
        c.anchor = Anchor.Center
        c.insets = new Insets(10, 10, 10, 10)
        layout(new BoxPanel(Orientation.Vertical){

            //CreateButton
            contents += CreateButton

            //LoadButton
            contents += LoadButton

            //SaveButton
            contents += SaveButton
        }) = c

        //SceneEditor
        c.fill = Fill.Both
        c.weightx = 0.1
        c.weighty = 1
        c.grid = (1, 0)
        c.anchor = Anchor.Center
        c.insets = new Insets(10, 10, 10, 10)
        layout(new BoxPanel(Orientation.Vertical){

            //MonsterList
            contents += new ScrollPane{
                contents = MonsterListTable
            }
        }) = c

        //SimulationManager
        c.fill = Fill.Horizontal
        c.weightx = 0.2
        c.weighty = 0
        c.grid = (2, 0)
        c.anchor = Anchor.Center
        c.insets = new Insets(10, 10, 10, 10)
        layout(new BoxPanel(Orientation.Vertical){

            //SceneStatus
            contents += new BoxPanel(Orientation.Horizontal){

                //SpeedLabel
                contents += new Label("Current Speed :")

                //SpeedValue
                contents += SpeedValue

                //TurnLabel
                contents += new Label("Turn Count :")

                //TurnValue
                contents += TurnValue
            }

            //SpeedSlider
            contents += SpeedSlider

            //PlayBox
            contents += new BoxPanel(Orientation.Horizontal){

                //StopButton
                contents += StopButton

                //PlayPauseButton
                contents += PlayPauseButton

                //NextButton
                contents += NextButton
            }
        }) = c

        //Inspector
        c.fill = Fill.Both
        c.weightx = 0.2
        c.weighty = 1
        c.grid = (3, 0)
        c.anchor = Anchor.Center
        c.insets = new Insets(10, 10, 10, 10)
        layout(new BoxPanel(Orientation.Horizontal){

            //BasicInfo
            contents += new BoxPanel(Orientation.Vertical){

                //NameLabel
                contents += NameLabel

                //HealthLabel
                contents += HealthLabel

                //PositionLabel
                contents += PositionLabel
            }

            //ActionListModel
            contents += new ScrollPane{
                contents = ActionListTable
            }

            //ActionLogModel
            contents += new ScrollPane{
                contents = ActionLogTable
            }
        }) = c


    }
    private val MainContainer = new GridBagPanel(){
        val c = new Constraints

        c.fill = Fill.Both
        c.weightx = 1
        c.weighty = 0.80
        c.grid = (0, 0)
        layout(Field) = c

        c.weightx = 1
        c.weighty = 0.20
        c.grid = (0, 1)
        layout(Menu) = c
    }
    contents = MainContainer
    maximize()

    //Définition du comportement des composants
    listenTo(CreateButton)
    listenTo(LoadButton)
    listenTo(SaveButton)
    listenTo(StopButton)
    listenTo(SpeedSlider)
    listenTo(PlayPauseButton)
    listenTo(NextButton)
    listenTo(Field.mouse.clicks)
    listenTo(Field.mouse.wheel)
    listenTo(Field.mouse.moves)

    reactions += {
        case ValueChanged(SpeedSlider) =>
            SpeedValue.text = SpeedSlider.value.toString


        case ButtonClicked(CreateButton) =>
            if(!isRunning) {
                val res = Dialog.showConfirmation(contents.head, "Current scene will be overwritten.\n Do you confirm ?")
                if (res == Dialog.Result.Ok) {
                    InitialScene = List()
                    top = -20
                    left = -20
                    bottom = 20
                    right = 20
                    selectedActor = null
                    updateView()
                }
            }
            else{
                Dialog.showMessage(contents.head, "Please stop the simulation before editing the scene")
            }


        case ButtonClicked(LoadButton) =>
            if(!isRunning){
                val res = Dialog.showConfirmation(contents.head, "Current scene will be overwritten.\n Do you confirm ?")
                if (res == Dialog.Result.Ok) {
                    val res = chooser.showOpenDialog(contents.head)
                    if(res == FileChooser.Result.Approve){
                        loadScene(chooser.selectedFile)
                        selectedActor = null
                        updateView()
                    }
                }
            }
            else{
                Dialog.showMessage(contents.head, "Please stop the simulation before editing the scene")
            }


        case ButtonClicked(SaveButton) =>
            if(!isRunning){
                val res = chooser.showSaveDialog(contents.head)
                if(res == FileChooser.Result.Approve){
                    saveScene(chooser.selectedFile)
                }
            }
            else{
                Dialog.showMessage(contents.head, "Please stop the simulation before editing the scene")
            }


        case ButtonClicked(StopButton) =>
            if(isRunning){
                isRunning = false
                isPaused = false
                currentSceneInstance = null
                selectedActor = null
                currentTurn = 0
                updateView()
            }


        case ButtonClicked(PlayPauseButton) =>
            if(isRunning){
                isPaused = !isPaused
            }
            else{
                isRunning = true
                isPaused = false
                startSimulation()
            }


        case ButtonClicked(NextButton) =>
            if(isRunning && isPaused){
                loadNextScene()
            }

        case e: MousePressed =>
            if(e.peer.getButton == 2){
                dragging = true
                lastPos = e.point
            }
            else if(e.peer.getButton == 3){
                if(!isRunning && MonsterListTable.selection.rows.leadIndex >= 0){
                    selectCadre = ((e.point.x,e.point.y),(e.point.x,e.point.y))
                }
            }

        case e: MouseReleased =>
            if(e.peer.getButton == 2){
                dragging = false
            }
            else if(e.peer.getButton == 3){
                if(!isRunning && selectCadre != null){

                    val actor = ActorType.values.toList(MonsterListTable.selection.rows.leadIndex)
                    val actorSize = ActorModel.from(actor).size
                    var point1 = ViewToField(
                            Math.min(selectCadre._1._1, selectCadre._2._1),
                            Math.min(selectCadre._1._2, selectCadre._2._2)
                    )
                    var point2 = ViewToField(
                            Math.max(selectCadre._1._1, selectCadre._2._1),
                            Math.max(selectCadre._1._2, selectCadre._2._2)
                    )
                    val actorMinMarge = 0.25 * actorSize
                    val actorMinDistance = 2 * (actorSize + actorMinMarge)

                    val xActorCount = ((point2._1 - point1._1)/actorMinDistance).toInt
                    val yActorCount = ((point2._2 - point1._2)/actorMinDistance).toInt

                    val xActorDistance =  (point2._1 - point1._1)/xActorCount
                    val yActorDistance =  (point2._2 - point1._2)/yActorCount

                    var bufferScene = InitialScene.toBuffer

                    point1 = (point1._1 + xActorDistance/2, point1._2 + yActorDistance/2)
                    point2 = (point2._1 - xActorDistance/2, point2._2 - yActorDistance/2)

                    for(i <- 0 until xActorCount){
                        for(j <- 0 until yActorCount){
                            val pos = (point1._1 + i * xActorDistance, point1._2 + j * yActorDistance)
                            bufferScene = bufferScene += new ActorSetup(actor, (pos._1.toFloat, pos._2.toFloat))
                        }
                    }

                    InitialScene = bufferScene.toList
                    selectedActor = null
                    selectCadre = null
                    updateView()
                }
            }

        case e: MouseDragged =>
            if(dragging){
                val viewHeight = Field.size.height
                val viewWidth = Field.size.width
                val fieldHeight = bottom - top
                val fieldWidth = right - left

                var distx: Double = lastPos.x - e.point.x
                var disty: Double = lastPos.y - e.point.y

                distx = distx * fieldWidth / viewWidth
                disty = disty * fieldHeight / viewHeight

                left += distx
                right += distx
                top += disty
                bottom += disty

                lastPos = e.point

                Field.repaint
            }

            if(selectCadre != null){
                selectCadre = (selectCadre._1,(e.point.x,e.point.y))
                updateView()
            }

        case e: MouseClicked =>
            val button = e.peer.getButton
            if(button == 1){
                selectedActor = getClickedActor(ViewToField(e.point.x, e.point.y))
                if(selectedActor != null && selectedActor.life == null) selectedActor.initialize(ActorModel.from(selectedActor.t))
                updateView()
            }
            else if(button == 2){
                if(!isRunning){
                    val actor = getClickedActorSetup(ViewToField(e.point.x, e.point.y))
                    if(actor != null){
                        val scene = InitialScene.toBuffer
                        val index = scene.indexOf(actor)
                        scene.remove(index, 1)
                        InitialScene = scene.toList
                        selectedActor = null
                    }
                    updateView()
                }
                else{
                    Dialog.showMessage(contents.head, "Please stop the simulation before editing the scene")
                }
            }
            else if(button == 3){
                //right click
                if(!isRunning){
                    if(MonsterListTable.selection.rows.leadIndex >= 0){
                        val pos = ViewToField(e.point.x, e.point.y)
                        val actor = ActorType.values.toList(MonsterListTable.selection.rows.leadIndex)
                        InitialScene = (InitialScene.toBuffer += new ActorSetup(actor, (pos._1.toFloat, pos._2.toFloat))).toList
                        currentSceneInstance = null
                        selectedActor = null
                        updateView()
                    }
                }
                else{
                    Dialog.showMessage(contents.head, "Please stop the simulation before editing the scene")
                }
            }

        case e: MouseWheelMoved =>
            val fieldHeight = bottom - top
            val fieldWidth = right - left

            top -= e.rotation*fieldHeight/10.0
            left -= e.rotation*fieldWidth/10.0
            bottom += e.rotation*fieldHeight/10.0
            right += e.rotation*fieldWidth/10.0
            updateView()

    }

}
