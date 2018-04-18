package UI

import java.awt.Color
import java.io._

import breeze.numerics.sqrt

import scala.swing._
import scala.swing.event._
import common._
import engine.{Actor, ActorModel}
import javax.swing.table.DefaultTableModel

class DDFightFrame extends MainFrame {

    //Variable pour la logique interne du simulateur
    private var InitialScene = List(
        new ActorSetup(ActorType.Solar, (-10, 0)),
        new ActorSetup(ActorType.OrcBarbarian, (10, 0)))
    private var channel: Channel[List[Actor]] = _
    private var currentSceneInstance : List[Actor] = List()
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

    //Fonction pour la logique interne du simulateur

    def getClickedActor(pos: (Double, Double)): Actor = {
        var res: Actor = null
        for (actor <- currentSceneInstance) {
            val dx = actor.pos._1 - pos._1
            val dy = actor.pos._2 - pos._2
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < actor.model.size) res = actor
        }
        res
    }

    def getClickedActorSetup(pos: (Double, Double)): ActorSetup = {
        var res: ActorSetup = null
        for (actor <- InitialScene) {
            val dx = actor.pos._1 - pos._1
            val dy = actor.pos._2 - pos._2
            val distance = sqrt(dx * dx + dy * dy)
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
        ActionListTable.repaint()
        ActionLogTable.repaint()
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
        //TODO
    }

    def loadNextScene(): Boolean = {
        if(isRunning && channel.getQueueSize()>0) {
            currentSceneInstance = channel.pop() match {
                case Some(s) => s
                case None => currentSceneInstance
            }
            currentTurn += 1
            TurnValue.text = currentTurn.toString
            selectedActor = null
            updateView()
            true
        }
        else false
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
    private val ActionListModel = new DefaultTableModel
    private val ActionListTable  = new Table{
        model = ActionListModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val ActionLogModel = new DefaultTableModel
    private val ActionLogTable = new Table{
        model = ActionLogModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val MonsterListModel = new MonsterListModel
    private val MonsterListTable = new Table {
        model = MonsterListModel
        selection.elementMode = Table.ElementMode.Row
    }
    private val HealthLabel = new Label("Health")
    private val NameLabel = new Label("Name")
    private val PositionLabel = new Label("Position")
    private val Inspector = new BoxPanel(Orientation.Horizontal){

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
    }
    private val Field = new Panel{
        override protected def paintComponent(g: Graphics2D): Unit = {
            //clean view
            g.setColor(Color.white)
            g.fillRect(0, 0, size.width, size.height)

            //draw mobs

            if(selectedActor !=null) {
                val model = selectedActor.model
                if (model.side == ActorSide.Angels) g.setColor(Color.blue)
                else g.setColor(Color.red)
                drawSelectedActor(g, (selectedActor.pos._1, selectedActor.pos._2), model.size)
            }

            if(currentSceneInstance.isEmpty && InitialScene.nonEmpty){
                for(actor <- InitialScene){
                    val model = ActorModel.from(actor.actorType)
                    if(model.side == ActorSide.Angels) g.setColor(Color.blue)
                    else g.setColor(Color.red)
                    drawActor(g, (actor.pos._1, actor.pos._2), model.size, 1)
                }
            }
            for(actor <- currentSceneInstance){
                val model = actor.model
                if(model.side == ActorSide.Angels) g.setColor(Color.blue)
                else g.setColor(Color.red)
                drawActor(g, (actor.pos._1, actor.pos._2), model.size, actor.life.current / actor.life.max)
            }
        }
    }
    private val Menu = new BoxPanel(Orientation.Horizontal){

        //SceneManager
        contents += new BoxPanel(Orientation.Vertical){

            //CreateButton
            contents += CreateButton

            //LoadButton
            contents += LoadButton

            //SaveButton
            contents += SaveButton
        }

        //SimulationManager
        contents += new BoxPanel(Orientation.Vertical){

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
        }

        //Inspector
        contents += Inspector

        //SceneEditor
        contents += new BoxPanel(Orientation.Vertical){

            //MonsterList
            contents += new ScrollPane{
                contents = MonsterListTable
            }
        }
    }
    private val WindowSplitContainer = new SplitPane(Orientation.Horizontal, Field,  Menu)
    contents = WindowSplitContainer
    WindowSplitContainer.dividerLocation = 0.75

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
                currentSceneInstance = List()
                selectedActor = null
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

        case e: MouseReleased =>
            if(e.peer.getButton == 2){
                dragging = false
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

        case e: MouseClicked =>
            val button = e.peer.getButton
            if(button == 1){
                selectedActor = getClickedActor(ViewToField(e.point.x, e.point.y))
                if(selectedActor != null){
                    HealthLabel.text = selectedActor.life.current.toString
                    NameLabel.text = selectedActor.model.actorType.toString
                    PositionLabel.text = selectedActor.pos.toString()
                }
                else{
                    HealthLabel.text = ""
                    NameLabel.text = ""
                    PositionLabel.text = ""
                }
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
                        currentSceneInstance = List()
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
