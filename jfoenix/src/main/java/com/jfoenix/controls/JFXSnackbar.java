/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jfoenix.controls;

import com.jfoenix.controls.ButtonType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @see <a href= "http://www.google.com/design/spec/components/snackbars-toasts.html#"> Snackbars & toasts</a>
 * <p>
 * The use of a javafx Popup or PopupContainer for notifications would seem intuitive but Popups are displayed in their
 * own dedicated windows and alligning the popup window and handling window on top layering is more trouble then it is
 * worth.
 */
public class JFXSnackbar extends StackPane {

    private Label toast;
    private JFXButton action;

    private Pane snackbarContainer;
    private Group popup;
    private ChangeListener<? super Number> sizeListener;

    private AtomicBoolean processingQueue = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<SnackbarEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private StackPane actionContainer;

    private Interpolator easeInterpolator = Interpolator.SPLINE(0.250, 0.100, 0.250, 1.000);
    private BorderPane content;
    private String activePseudoClass = null;

    public JFXSnackbar() {
        this(null);
    }

    public JFXSnackbar(Pane snackbarContainer) {

        content = new BorderPane();
        toast = new Label();
        toast.setMinWidth(Control.USE_PREF_SIZE);
        toast.getStyleClass().add("jfx-snackbar-toast");
        toast.setWrapText(true);
        StackPane toastContainer = new StackPane(toast);
        toastContainer.setPadding(new Insets(20));
        content.setLeft(toastContainer);

        action = new JFXButton();
        action.setMinWidth(Control.USE_PREF_SIZE);
        action.setButtonType(ButtonType.FLAT);
        action.getStyleClass().add("jfx-snackbar-action");

        // actions will be added upon showing the snackbar if needed
        actionContainer = new StackPane(action);
        actionContainer.setPadding(new Insets(0, 10, 0, 0));
        content.setRight(actionContainer);

        toast.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            if (this.getPrefWidth() == -1) {
                return this.getPrefWidth();
            }
            double actionWidth = actionContainer.isVisible() ? actionContainer.getWidth() : 0.0;
            return this.prefWidthProperty().get() - actionWidth;
        }, this.prefWidthProperty(), actionContainer.widthProperty(), actionContainer.visibleProperty()));

        //bind the content's height and width from this snackbar allowing the content's dimensions to be set externally
        content.prefWidthProperty().bind(this.prefWidthProperty());
        content.getStyleClass().add("jfx-snackbar-content");
        //setting a shadow enlarges the snackbar height leaving a gap below it
        //JFXDepthManager.setDepth(content, 4);

        //wrap the content in a group so that the content is managed inside its own container
        //but the group is not managed in the snackbarContainer so it does not affect any layout calculations
        popup = new Group();
        popup.getChildren().add(content);
        popup.setManaged(false);
        popup.setVisible(false);
        popup.idProperty().bind(this.idProperty());
        this.getStyleClass().addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    popup.getStyleClass().addAll(c.getAddedSubList());
                }
                if (c.wasRemoved()) {
                    popup.getStyleClass().removeAll(c.getRemoved());
                }
            }
        });
        sizeListener = (o, oldVal, newVal) -> refreshPopup();

        // register the container before resizing it
        registerSnackbarContainer(snackbarContainer);

        // resize the popup if its layout has been changed
        popup.layoutBoundsProperty().addListener((o, oldVal, newVal) -> refreshPopup());

        addEventHandler(SnackbarEvent.SNACKBAR, e -> enqueue(e));

        //This control actually orchestrates the popup logic and is never visibly displayed.
        this.setVisible(false);
        this.setManaged(false);

    }

    /***************************************************************************
     * * Setters / Getters * *
     **************************************************************************/

    public Pane getPopupContainer() {
        return snackbarContainer;
    }


    /***************************************************************************
     * * Public API * *
     **************************************************************************/

    public void registerSnackbarContainer(Pane snackbarContainer) {

        if (snackbarContainer != null) {
            if (this.snackbarContainer != null) {
                //since listeners are added the container should be properly registered/unregistered
                throw new IllegalArgumentException("Snackbar Container already set");
            }

            this.snackbarContainer = snackbarContainer;
            this.snackbarContainer.getChildren().add(popup);
            this.snackbarContainer.heightProperty().addListener(sizeListener);
            this.snackbarContainer.widthProperty().addListener(sizeListener);
        }

    }

    public void unregisterSnackbarContainer(Pane snackbarContainer) {

        if (snackbarContainer != null) {
            if (this.snackbarContainer == null) {
                throw new IllegalArgumentException("Snackbar Container not set");
            }

            this.snackbarContainer.getChildren().remove(popup);
            this.snackbarContainer.heightProperty().removeListener(sizeListener);
            this.snackbarContainer.widthProperty().removeListener(sizeListener);
            this.snackbarContainer = null;
        }
    }

    public void show(String toastMessage, long timeout) {
        this.show(toastMessage, null, timeout, null);
    }

    public void show(String message, String actionText, EventHandler<? super MouseEvent> actionHandler) {
        this.show(message, actionText, -1, actionHandler);
    }

    public void show(String toastMessage, String pseudoClass, long timeout) {
        this.show(toastMessage, pseudoClass,null, timeout, null);
    }

    public void show(String message, String pseudoClass, String actionText, EventHandler<? super MouseEvent> actionHandler) {
        this.show(message, pseudoClass, actionText, -1, actionHandler);
    }

    public void show(String message,  String actionText, long timeout, EventHandler<? super MouseEvent> actionHandler){
        this.show(message,null,  actionText, -1, actionHandler);
    }

    public void show(String message, String pseudoClass, String actionText, long timeout, EventHandler<? super MouseEvent> actionHandler) {
        toast.setText(message);
        if (actionText != null && !actionText.isEmpty()) {
            action.setVisible(true);
            actionContainer.setVisible(true);
            actionContainer.setManaged(true);
            // to force updating the layout bounds
            action.setText("");
            action.setText(actionText);
            action.setOnMouseClicked(actionHandler);
        } else {
            actionContainer.setVisible(false);
            actionContainer.setManaged(false);
            action.setVisible(false);
        }
        Timeline animation = getTimeline(timeout);
        if(pseudoClass!=null){
            activePseudoClass = pseudoClass;
            content.pseudoClassStateChanged(PseudoClass.getPseudoClass(activePseudoClass), true);
        }
        animation.play();
    }

    private Timeline getTimeline(long timeout) {
        Timeline animation;
        if (timeout <= 0) {
            animation = new Timeline(
                new KeyFrame(
                    Duration.ZERO,
                    e -> popup.toBack(),
                    new KeyValue(popup.visibleProperty(), false, Interpolator.EASE_BOTH),
                    new KeyValue(popup.translateYProperty(), popup.getLayoutBounds().getHeight(), easeInterpolator),
                    new KeyValue(popup.opacityProperty(), 0, easeInterpolator)
                ),
                new KeyFrame(
                    Duration.millis(10),
                    e -> popup.toFront(),
                    new KeyValue(popup.visibleProperty(), true, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(300),
                    new KeyValue(popup.opacityProperty(), 1, easeInterpolator),
                    new KeyValue(popup.translateYProperty(), 0, easeInterpolator)
                )
            );
            animation.setCycleCount(1);
        } else {
            animation = new Timeline(
                new KeyFrame(
                    Duration.ZERO,
                    (e) -> popup.toBack(),
                    new KeyValue(popup.visibleProperty(), false, Interpolator.EASE_BOTH),
                    new KeyValue(popup.translateYProperty(), popup.getLayoutBounds().getHeight(), easeInterpolator),
                    new KeyValue(popup.opacityProperty(), 0, easeInterpolator)
                ),
                new KeyFrame(
                    Duration.millis(10),
                    (e) -> popup.toFront(),
                    new KeyValue(popup.visibleProperty(), true, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(300),
                    new KeyValue(popup.opacityProperty(), 1, easeInterpolator),
                    new KeyValue(popup.translateYProperty(), 0, easeInterpolator)
                ),
                new KeyFrame(Duration.millis(timeout / 2))
            );
            animation.setAutoReverse(true);
            animation.setCycleCount(2);
            animation.setOnFinished((e) -> {
                resetPseudoClass();
                processSnackbars();
            });
        }
        return animation;
    }

    public void close() {
        Timeline animation = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                e -> popup.toFront(),
                new KeyValue(popup.opacityProperty(), 1, easeInterpolator),
                new KeyValue(popup.translateYProperty(), 0, easeInterpolator)
            ),
            new KeyFrame(
                Duration.millis(290),
                new KeyValue(popup.visibleProperty(), true, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.millis(300),
                e -> popup.toBack(),
                new KeyValue(popup.visibleProperty(), false, Interpolator.EASE_BOTH),
                new KeyValue(popup.translateYProperty(),
                    popup.getLayoutBounds().getHeight(),
                    easeInterpolator),
                new KeyValue(popup.opacityProperty(), 0, easeInterpolator)
            )
        );
        animation.setCycleCount(1);
        animation.setOnFinished(e -> {
            resetPseudoClass();
            processSnackbars();
        });
        animation.play();
    }

    private void resetPseudoClass() {
        if(activePseudoClass!=null) {
            content.pseudoClassStateChanged(PseudoClass.getPseudoClass(activePseudoClass), false);
            activePseudoClass = null;
        }
    }

    private void processSnackbars() {
        SnackbarEvent qevent = eventQueue.poll();
        if (qevent != null) {
            if (qevent.isPersistent()) {
                show(qevent.getMessage(), qevent.getpseudoClass(), qevent.getActionText(), qevent.getActionHandler());
            } else {
                show(qevent.getMessage(), qevent.getpseudoClass(), qevent.getActionText(), qevent.getTimeout(), qevent.getActionHandler());
            }
        } else {
            //The enqueue method and this listener should be executed sequentially on the FX Thread so there
            //should not be a race condition
            processingQueue.getAndSet(false);
        }
    }


    public void refreshPopup() {
        Bounds contentBound = popup.getLayoutBounds();
        double offsetX = Math.ceil(snackbarContainer.getWidth() / 2) - Math.ceil(contentBound.getWidth() / 2);
        double offsetY = snackbarContainer.getHeight() - contentBound.getHeight();
        popup.setLayoutX(offsetX);
        popup.setLayoutY(offsetY);

    }

    public void enqueue(SnackbarEvent event) {
        eventQueue.add(event);
        if (processingQueue.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                SnackbarEvent qevent = eventQueue.poll();
                if (qevent != null) {
                    if (qevent.isPersistent()) {
                        show(qevent.getMessage(),
                            qevent.getpseudoClass(),
                            qevent.getActionText(),
                            qevent.getActionHandler());
                    } else {
                        show(qevent.getMessage(),
                            qevent.getpseudoClass(),
                            qevent.getActionText(),
                            qevent.getTimeout(),
                            qevent.getActionHandler());
                    }
                }
            });
        }
    }


    /***************************************************************************
     * * Event API * *
     **************************************************************************/

    public static class SnackbarEvent extends Event {
        public static final EventType<SnackbarEvent> SNACKBAR = new EventType<>(Event.ANY, "SNACKBAR");

        private final String message;
        private final String actionText;
        private final String pseudoClass;
        private final long timeout;
        private final boolean persistent;
        private final EventHandler<? super MouseEvent> actionHandler;

        public SnackbarEvent(String message) {
            this(message, null, 3000, false, null);
        }
        public SnackbarEvent(String message, String pseudoClass) {
            this(message, pseudoClass,null, 3000, false, null);
        }
        public SnackbarEvent(String message, String actionText, long timeout, boolean persistent, EventHandler<? super MouseEvent> actionHandler) {
            this(message, null,null, 3000, false, null);
        }
        public SnackbarEvent(String message,String pseudoClass, String actionText, long timeout, boolean persistent, EventHandler<? super MouseEvent> actionHandler) {
            super(SNACKBAR);
            this.message = message;
            this.pseudoClass = pseudoClass;
            this.actionText = actionText;
            this.timeout = timeout < 1 ? 3000 : timeout;
            this.actionHandler = actionHandler;
            this.persistent = persistent;
        }

        public String getMessage() {
            return message;
        }

        public String getActionText() {
            return actionText;
        }

        public long getTimeout() {
            return timeout;
        }

        public EventHandler<? super MouseEvent> getActionHandler() {
            return actionHandler;
        }

        @Override
        public EventType<? extends SnackbarEvent> getEventType() {
            return (EventType<? extends SnackbarEvent>) super.getEventType();
        }

        public boolean isPersistent() {
            return persistent;
        }

        public String getpseudoClass() {
            return pseudoClass;
        }
    }
}

