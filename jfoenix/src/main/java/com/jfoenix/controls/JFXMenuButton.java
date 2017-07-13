/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jfoenix.controls;

import com.jfoenix.converters.ButtonTypeConverter;
import com.jfoenix.skins.JFXMenuButtonSkin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Skin;
import javafx.scene.paint.Paint;

/**
 *
 * @author gcmartins
 */
public class JFXMenuButton extends MenuButton {
    public JFXMenuButton() {
        initialize();

        // init in scene builder workaround ( TODO : remove when JFoenix is well integrated in scenebuilder by gluon )
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTraceElements.length && i < 15; i++) {
            if (stackTraceElements[i].getClassName().toLowerCase().contains(".scenebuilder.kit.fxom.")) {
                this.setText("MenuButton");
                break;
            }
        }
    }
    
    public JFXMenuButton(String text) {
        super(text);
        initialize();
    }
    
    public JFXMenuButton(String text, Node graphic) {
        super(text, graphic);
        initialize();
    }
    
    private void initialize() {
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);
    }
    
    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXMenuButtonSkin(this);
    }
    
    private ObjectProperty<Paint> ripplerFill = new SimpleObjectProperty<>(null);
    
    public final ObjectProperty<Paint> ripplerFillProperty() {
        return this.ripplerFill;
    }
    
     public final void setRipplerFill(final Paint ripplerFill) {
        this.ripplerFillProperty().set(ripplerFill);
    }
     
    public final Paint getRipplerFill() {
        return this.ripplerFillProperty().get();
    }
    
    private static final String DEFAULT_STYLE_CLASS = "jfx-menu-button";
    
    private StyleableObjectProperty<ButtonType> buttonType = new SimpleStyleableObjectProperty<>(
        StyleableProperties.BUTTON_TYPE,
        JFXMenuButton.this,
        "buttonType",
        ButtonType.FLAT);

    public ButtonType getButtonType() {
        return buttonType == null ? ButtonType.FLAT : buttonType.get();
    }

    public StyleableObjectProperty<ButtonType> buttonTypeProperty() {
        return this.buttonType;
    }

    public void setButtonType(ButtonType type) {
        this.buttonType.set(type);
    }

    private static class StyleableProperties {
        private static final CssMetaData<JFXMenuButton, ButtonType> BUTTON_TYPE =
            new CssMetaData<JFXMenuButton, ButtonType>("-jfx-button-type", ButtonTypeConverter.getInstance(), ButtonType.FLAT) {
                @Override
                public boolean isSettable(JFXMenuButton control) {
                    return control.buttonType == null || !control.buttonType.isBound();
                }

                @Override
                public StyleableProperty<ButtonType> getStyleableProperty(JFXMenuButton control) {
                    return control.buttonTypeProperty();
                }
            };

        private static final List<CssMetaData<? extends Styleable, ?>> CHILD_STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Control.getClassCssMetaData());
            Collections.addAll(styleables,
                BUTTON_TYPE
            );
            CHILD_STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    // inherit the styleable properties from parent
    private List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        if (STYLEABLES == null) {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Control.getClassCssMetaData());
            styleables.addAll(getClassCssMetaData());
            styleables.addAll(Labeled.getClassCssMetaData());
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
        return STYLEABLES;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.CHILD_STYLEABLES;
    }
}
