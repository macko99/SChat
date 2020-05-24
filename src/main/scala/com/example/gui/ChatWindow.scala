package com.example.gui
import java.util.Calendar
import com.example.GUIClient
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color._
import scalafx.scene.text.Text

object GUIStyles {
  val richButtonStyle: String =
    "-fx-background-color:" +
      "#000000," +
      "linear-gradient(#7ebcea, #2f4b8f)," +
      "linear-gradient(#426ab7, #263e75)," +
      "linear-gradient(#395cab, #223768);" +
      "-fx-background-insets: 0,1,2,3;" +
      "-fx-background-radius: 3,2,2,2;" +
      "-fx-padding: 12 30 12 30;" +
      "-fx-text-fill: white;" +
      "-fx-font: bold 12pt sans-serif;"

  val commonTextStyle: String =
    "font-family: Verdana;" +
      "font-size: 11px;" +
      "color: #555555;" +
      "line-height: 1.5;" +
      "letter-spacing: .25px;"

  val tabPaneStyle: String =
    "-fx-background-color:" +
      "-fx-outer-border," +
      "-fx-inner-border," +
      "derive(-fx-color, -20%);" +
      "-fx-effect: " +
      "innershadow(two-pass-box , rgba(0,0,0,0.6) , 4, 0.0 , 0 , 0);"
}

object ChatClientWindow extends JFXApp {

  val loginDialog: LoginWindow= LoginWindow(stage)
  loginDialog.initializeDialog()

  val chatOutputArea: TextArea = new TextArea {
    editable = false
    focusTraversable = false
    style = GUIStyles.commonTextStyle
  }

  val chatInputField: TextField = new TextField {
    text.set("")
    onKeyPressed = (a: KeyEvent) => a.code match {
      case KeyCode.Enter =>
        val message = text() + "\n"
        text.set("")
        client.sendMessage(message)
      case _ =>
    }
  }

  val mainChat: VBox = new VBox {
    padding = Insets(5)
    alignment = Pos.TopCenter
    children = Seq(chatOutputArea, chatInputField)
  }

  val defaultRoomTab: Tab = new Tab {
    text = "room: " + loginDialog.roomId.toString
    content = mainChat
    onClosed = handle(stopApp())
    closable = false
  }

  val tabPane: TabPane = new TabPane {
    tabs = List(defaultRoomTab)
    style = GUIStyles.tabPaneStyle
  }

  val serverAddress: String = "ws://" + loginDialog.hostname + ":" + loginDialog.port.toString + "/"
  val client: GUIClient = GUIClient(serverAddress)
  client.connectToRoom(s"schat/room/" + loginDialog.roomId + "?name=" + loginDialog.username, chatOutputArea)

  stage = new PrimaryStage {
    title = "SChat"
    minWidth = 900
    minHeight = 600
    scene = new Scene(900, 600) {
      root = new VBox {
        spacing = 33
        padding = Insets(30)
        alignment = Pos.Center
        style = "-fx-background-color: #223162;"

        children = Seq(
          new Text {
            text = "Hello SChat!"
            style = "-fx-font: bold 32pt sans-serif"
            fill = White

          }, tabPane,
          new Text {
            text = s"Logged as: ${loginDialog.username}\nLogin time: ${Calendar.getInstance().getTime.toString}"
            style = "-fx-font: italic 11pt sans-serif"
            fill = White
          })
      }
    }
    onCloseRequest = _ => {
      client.exit()
      Platform.exit()
      sys.exit()
    }
  }

  Platform.runLater(() => chatInputField.requestFocus())
}