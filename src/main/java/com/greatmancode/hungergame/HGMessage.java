package com.greatmancode.hungergame;

import me.ampayne2.ultimategames.api.message.Message;

public enum HGMessage implements Message {
    GRACE_START("GraceStart", "&4You have a 30 second grace period!"),
    GRACE_END("GraceEnd", "&4The grace period has ended."),
    END("End", "%s won HungerGames!"),
    KILL("Kill", "%s killed %s!"),
    DEATH("Death", "%s died!"),
    LEAVE("Leave", "%s left the game!");

    private String message;
    private final String path;
    private final String defaultMessage;

    private HGMessage(String path, String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
        this.message = defaultMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDefault() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return message;
    }
}
