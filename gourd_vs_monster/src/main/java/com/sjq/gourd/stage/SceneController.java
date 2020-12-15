package com.sjq.gourd.stage;

import com.sjq.gourd.bullet.Bullet;
import com.sjq.gourd.client.GameClient;
import com.sjq.gourd.client.MsgController;
import com.sjq.gourd.collision.Collision;
import com.sjq.gourd.constant.Constant;
import com.sjq.gourd.creature.CreatureClass;
import com.sjq.gourd.creature.GourdClass;
import com.sjq.gourd.creature.MonsterClass;
import com.sjq.gourd.protocol.Msg;
import com.sjq.gourd.protocol.NoParseMsg;
import com.sjq.gourd.protocol.PositionNotifyMsg;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.*;

import com.sjq.gourd.server.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class SceneController {
    @FXML
    private Pane StartScene;
    @FXML
    private Pane ConnectScene;
    @FXML
    private Pane fightScene;
    @FXML
    private Pane gourdMapPane;
    @FXML
    private Pane monsterMapPane;
    @FXML
    private TextField ServerPortText2;
    @FXML
    private TextField ServerIpText;
    @FXML
    private TextField ServerPortText1;

    private final Random randomNum = new Random(System.currentTimeMillis());
    private Text notificationMidText;
    private DataInputStream in;
    private DataOutputStream out;
    private MsgController msgController;
    private String campType;
    private HashMap<Integer, GourdClass> gourdFamily = new HashMap<Integer, GourdClass>();
    private HashMap<Integer, MonsterClass> monsterFamily = new HashMap<Integer, MonsterClass>();

    private CreatureClass selectOwnCampCreature;
    PositionXY beginPosition = new PositionXY(0, 0);

    ArrayList<Bullet> bulletList = new ArrayList<>();

    @FXML
    void AboutUsMouseClickEvent(MouseEvent event) {
        System.out.println("about");
    }

    @FXML
    void ExitMouseClickEvent(MouseEvent event) {
        System.out.println("exit");
    }

    @FXML
    void LocalPlaybackMouseClickEvent(MouseEvent event) {
        System.out.println("localplay");
    }

    @FXML
    void NetPlayMouseClickEvent(MouseEvent event) {
        StartScene.setVisible(false);
        StartScene.setDisable(true);
        ConnectScene.setVisible(true);
        ConnectScene.setDisable(false);
    }

    @FXML
    void ConnectServerMouseClick(MouseEvent event) {
        String ipString = ServerIpText.getText();
        String portString = ServerPortText1.getText();

        String ipPattern = "\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}";
        String portPattern = "\\d{4,5}";

        boolean ipMatch = Pattern.matches(ipPattern, ipString);
        boolean portMatch = Pattern.matches(portPattern, portString);

        if (!ipMatch && !portMatch) {
            System.out.println("请输入规范的ip和port地址");
        }
        try {
            ConnectScene.setVisible(false);
            ConnectScene.setDisable(true);
            fightScene.setVisible(true);
            fightScene.setDisable(false);
            new GameClient(ipString, Integer.parseInt(portString), this).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void CreateServerMouseClick(MouseEvent event) {
        String text = ServerPortText2.getText();
        String portPattern = "\\d{4,5}";

        boolean isMatch = Pattern.matches(portPattern, text);
        if (!isMatch) {
            System.out.println("创建服务器端口不符合规范，请重新输入");
            return;
        }
        int port = Integer.parseInt(text);
        if (port < 5001 || port > 65535) {
            System.out.println("创建服务器端口不符合规范，请重新输入");
            return;
        }

        try {
            Thread serverThread = new GameServer(port);
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initGameSceneController(DataInputStream in, DataOutputStream out, String campType,
                                        HashMap<Integer, GourdClass> gourdFamily,
                                        HashMap<Integer, MonsterClass> monsterFamily) {
        this.in = in;
        this.out = out;
        this.campType = campType;
        this.gourdFamily = gourdFamily;
        this.monsterFamily = monsterFamily;
        msgController = new MsgController(gourdFamily, monsterFamily);
        notificationMidText = new Text();
        notificationMidText.setText("等待其他玩家加入");
        notificationMidText.setFont(Font.font("FangSong", 30));
        notificationMidText.setTextAlignment(TextAlignment.CENTER);
        double width = notificationMidText.getBoundsInLocal().getWidth();
        System.out.println(width);
        notificationMidText.setLayoutX(600 - width / 2);
        notificationMidText.setLayoutY(260);
        gourdMapPane.getChildren().add(notificationMidText);
        monsterMapPane.getChildren().add(notificationMidText);

        Line midLine = setLine(0, 0, 0, 700, 600 , 0,"solid");
        gourdMapPane.getChildren().add(midLine);
        monsterMapPane.getChildren().add(midLine);

        for(double x = 75; x < 600; x += 75) {
            Line tempLine = setLine(0, 0, 0, 700, x, 0, "dash");
            gourdMapPane.getChildren().add(tempLine);
        }
        for(double y = 70; y < 700; y += 70) {
            Line tempLine = setLine(0, 0, 600, 0, 0, y, "dash");
            gourdMapPane.getChildren().add(tempLine);
        }
        for(double x = 675; x < 1200; x += 75) {
            Line tempLine = setLine(0, 0, 0, 700, x, 0, "dash");
            monsterMapPane.getChildren().add(tempLine);
        }
        for(double y = 70; y < 700; y += 70) {
            Line tempLine = setLine(0, 0, 600, 0, 0, y, "dash");
            monsterMapPane.getChildren().add(tempLine);
        }
    }

    public Line setLine(double startX, double startY, double endX, double endY,
                        double layoutX, double layoutY, String lineType) {
        Line resLine = new Line(startX, startY, endX, endY);
        resLine.setLayoutX(layoutX);
        resLine.setLayoutY(layoutY);
        if(lineType.equals("dash"))
            resLine.getStrokeDashArray().addAll(10d, 10d);
        return resLine;
    }

    public void gourdStartGame() {
        gourdMapPane.setVisible(true);
        gourdMapPane.setDisable(false);
        monsterMapPane.setVisible(false);
        monsterMapPane.setDisable(true);
        waitForAnother();
    }

    public void waitForAnother() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int msgType = in.readInt();
                        if (msgType == Msg.PREPARE_GAME_MSG) break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                gourdPrepareForGame();
            }
        }).start();
    }

    public void gourdPrepareForGame() {
        for (GourdClass gourdMember : gourdFamily.values()) {
            gourdMember.getCreatureImageView().setVisible(true);
            gourdMember.getCreatureImageView().setDisable(false);
            gourdMember.getCreatureImageView().setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (selectOwnCampCreature != null) {
                        selectOwnCampCreature.setCreatureImageView();
                    }
                    beginPosition.setPosition(event.getX(), event.getY());
                    gourdMember.setSelectCreatureImageView();
                    selectOwnCampCreature = gourdMember;
                }
            });
            gourdMember.getCreatureImageView().setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    PositionXY currentPosition = new PositionXY(event.getSceneX(), event.getSceneY());
                    double deltaX = currentPosition.X - beginPosition.X;
                    double deltaY = currentPosition.Y - beginPosition.Y;
                    if(deltaX > Constant.FIGHT_PANE_WIDTH - Constant.CREATURE_IMAGE_WIDTH)
                        deltaX = Constant.FIGHT_PANE_WIDTH - Constant.CREATURE_IMAGE_WIDTH;
                    deltaX = (deltaX < 0) ? 0 : deltaX;
                    deltaY = (deltaY < 0) ? 0 : deltaY;
                    if(deltaY > Constant.FIGHT_PANE_HEIGHT - gourdMember.getImageHeight())
                        deltaY = Constant.FIGHT_PANE_HEIGHT - gourdMember.getImageHeight();
                    gourdMember.getCreatureImageView().setLayoutX(deltaX);
                    gourdMember.getCreatureImageView().setLayoutY(deltaY);
                }
            });
        }
        while (true) {
            try {
                int msgType = in.readInt();
                if (msgType == Msg.COUNT_DOWN_MSG) {
                    msgController.getMsgClass(msgType, in);
                    notificationMidText.setText(String.valueOf(msgController.getTimeRemaining()));
                } else if (msgType == Msg.START_GAME_MSG) {
                    for (Map.Entry<Integer, GourdClass> entry : gourdFamily.entrySet()) {
                        int creatureId = entry.getKey();
                        GourdClass gourdMember = entry.getValue();
                        ImageView tempImageView = gourdMember.getCreatureImageView();
                        tempImageView.setOnMouseDragged(null);
                        double width = tempImageView.getFitWidth();
                        double layoutX = tempImageView.getLayoutX();
                        if (layoutX + width > 600) {
                            tempImageView.setLayoutX(600 - width);
                            tempImageView.setLayoutY(randomNum.nextDouble() * 600);
                        }
                        new PositionNotifyMsg("Gourd", creatureId,
                                tempImageView.getLayoutX(), tempImageView.getLayoutY()).sendMsg(out);
                    }
                    new NoParseMsg(Msg.FINISH_FLAG_MSG).sendMsg(out);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {
            try {
                int msgType = in.readInt();
                System.out.println(msgType);
                if (msgType == Msg.POSITION_NOTIFY_MSG) {
                    msgController.getMsgClass(msgType, in);
                } else if (msgType == Msg.START_GAME_MSG) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gourdMapPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                PositionXY curPosition = new PositionXY(event.getSceneX(), event.getSceneY());
                double deltaX = curPosition.X - beginPosition.X;
                double deltaY = curPosition.Y - beginPosition.Y;
                deltaX = (deltaX > 1120) ? 1120 : deltaX;
                deltaX = (deltaX < 0) ? 0 : deltaX;
                deltaY = (deltaY < 0) ? 0 : deltaY;
                deltaY = (deltaY > 600) ? 600 : deltaY;
                ImageView tempImageView = selectOwnCampCreature.getCreatureImageView();
                deltaX -= tempImageView.getLayoutX();
                deltaY -= tempImageView.getLayoutY();
                tempImageView.setLayoutX(tempImageView.getLayoutX() + deltaX);
                tempImageView.setLayoutY(tempImageView.getLayoutY() + deltaY);
            }
        });
        gourdStartFight();
    }

    public void gourdStartFight() {
        while(true) {
            try {
                for(GourdClass gourdMember : gourdFamily.values()) {
                    gourdMember.randomMove();
                    gourdMember.draw();
                    Bullet bulletAttack = gourdMember.aiAttack();
                    if(bulletAttack != null) {
                        for(Bullet bullet : bulletList) {
                            if(!bullet.isValid()) {
                                bullet.changeBullet(bulletAttack);
                                break;
                            }
                        }
                    }
                    for (Bullet bullet : bulletList) {
                        if (bullet.isValid()) {
                            Collision collision = bullet.move();
                            bullet.draw();
                            if (collision != null)
                                collision.collisionEvent();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void monsterStartGame() {
        monsterMapPane.setVisible(true);
        monsterMapPane.setDisable(false);
        gourdMapPane.setVisible(false);
        gourdMapPane.setDisable(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                monsterPrepareForGame();
            }
        }).start();
    }

    public void monsterPrepareForGame() {
        for (MonsterClass monsterMember : monsterFamily.values()) {
            monsterMember.getCreatureImageView().setVisible(true);
            monsterMember.getCreatureImageView().setDisable(false);
            monsterMember.getCreatureImageView().setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (selectOwnCampCreature != null) {
                        selectOwnCampCreature.setCreatureImageView();
                    }
                    beginPosition.setPosition(event.getX(), event.getY());
                    monsterMember.setSelectCreatureImageView();
                    selectOwnCampCreature = monsterMember;
                }
            });
            monsterMember.getCreatureImageView().setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    PositionXY currentPosition = new PositionXY(event.getSceneX(), event.getSceneY());
                    double deltaX = currentPosition.X - beginPosition.X;
                    double deltaY = currentPosition.Y - beginPosition.Y;
                    deltaX = (deltaX > 520) ? 520 : deltaX;
                    deltaX = (deltaX < 0) ? 0 : deltaX;
                    deltaY = (deltaY < 0) ? 0 : deltaY;
                    deltaY = (deltaY > 600) ? 600 : deltaY;
                    monsterMember.getCreatureImageView().setLayoutX(deltaX);
                    monsterMember.getCreatureImageView().setLayoutY(deltaY);
                }
            });
        }
        while (true) {
            try {
                int msgType = in.readInt();
                System.out.println(msgType);
                if (msgType == Msg.COUNT_DOWN_MSG) {
                    msgController.getMsgClass(msgType, in);
                    notificationMidText.setText(String.valueOf(msgController.getTimeRemaining()));
                } else if (msgType == Msg.START_GAME_MSG) {
                    if (campType.equals("Gourd")) {
                        for (Map.Entry<Integer, GourdClass> entry : gourdFamily.entrySet()) {
                            int creatureId = entry.getKey();
                            GourdClass gourdMember = entry.getValue();
                            ImageView tempImageView = gourdMember.getCreatureImageView();
                            new PositionNotifyMsg("Gourd", creatureId,
                                    tempImageView.getLayoutX(), tempImageView.getLayoutY()).sendMsg(out);
                        }
                    } else if (campType.equals("Monster")) {
                        for (Map.Entry<Integer, MonsterClass> entry : monsterFamily.entrySet()) {
                            int creatureId = entry.getKey();
                            MonsterClass monsterMember = entry.getValue();
                            ImageView tempImageView = monsterMember.getCreatureImageView();
                            new PositionNotifyMsg("Monster", creatureId,
                                    tempImageView.getLayoutX(), tempImageView.getLayoutY()).sendMsg(out);
                        }
                    }
                    new NoParseMsg(Msg.FINISH_FLAG_MSG).sendMsg(out);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {
            try {
                int msgType = in.readInt();
                if (msgType == Msg.POSITION_NOTIFY_MSG) {
                    msgController.getMsgClass(msgType, in);
                } else if (msgType == Msg.START_GAME_MSG) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("fight");
    }

    public void addImageViewToFightMapPane(ImageView tempImageView) {
        fightScene.getChildren().add(tempImageView);
    }

    public void addImageViewToGourdMapPane(ImageView tempImageView) {
        gourdMapPane.getChildren().add(tempImageView);
    }

    public void addImageViewToMonsterMapPane(ImageView tempImageView) {
        monsterMapPane.getChildren().add(tempImageView);
    }


    public void addProgressBarToGourdMapPane(ProgressBar progressBar) {
        gourdMapPane.getChildren().add(progressBar);
    }

    public void addProgressBarToMonsterMapPane(ProgressBar progressBar) {
        monsterMapPane.getChildren().add(progressBar);
    }

    public Pane getGourdMapPane() {
        return gourdMapPane;
    }

    public Pane getMonsterMapPane() {
        return monsterMapPane;
    }

    public void monsterStartFight() {
        while(true) {
            try {
                for(MonsterClass monsterMember : monsterFamily.values()) {
                    monsterMember.randomMove();
                    monsterMember.draw();
                    Bullet bulletAttack = monsterMember.aiAttack();
                    if(bulletAttack != null) {
                        for(Bullet bullet : bulletList) {
                            if(!bullet.isValid()) {
                                bullet.changeBullet(bulletAttack);
                                break;
                            }
                        }
                    }
                    for (Bullet bullet : bulletList) {
                        if (bullet.isValid()) {
                            Collision collision = bullet.move();
                            bullet.draw();
                            if (collision != null)
                                collision.collisionEvent();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
