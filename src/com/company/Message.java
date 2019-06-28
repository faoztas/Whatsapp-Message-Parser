package com.company;

import java.util.Date;

public class Message {

    private final Date sentDate;
    private String author;
    private String text;

    public Message(Date sentDate, String author, String text){
        this.sentDate = sentDate;
        this.author = author;
        this.text = text;
    }

    public void setAuthor(String author){
        this.author = author;
    }

    public void addMore(String text){
        this.text += text;
    }

    public String getText(){
        return text;
    }

    public String getAuthor(){
        return author;
    }

    public Date getSentDate(){
        return sentDate;
    }

}
