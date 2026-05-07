package com.webhook.receiver.slack.vo.sender;

import java.util.ArrayList;
import java.util.List;

public class SlackAttachment {

    private String color= "#f54242";
    private String fallback;
    private String title;
    private String text;
    private String title_link;
    private List<Field> fields = new ArrayList<>();

    public SlackAttachment() {
    }

    public SlackAttachment(String fallback, String title, String text) {
        this.fallback = fallback;
        this.title = title;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getFallback() {
        return fallback;
    }

    public String getColor() {
        return color;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle_link() {
        return title_link;
    }

    public void setTitle_link(String title_link) {
        this.title_link = title_link;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fieldList) {
        this.fields = fieldList;
    }

    public void addField(Field field) {
        this.fields.add(field);
    }
}
