package com.ifcifc.jsondb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.UUID;

public abstract class BaseModel implements Cloneable, Serializable {
    private static Gson GSON;
    private final UUID id;

    public BaseModel(){
        this.id = null;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    protected Gson getGson(){
        if(GSON==null){
            GsonBuilder builder = new GsonBuilder();
            GSON = builder.create();
        }
        return GSON;
    }

    @Override
    public String toString() {
        return "<"+this.getClass().getSimpleName()+">: " + getGson().toJson(this);
    }

    public String toJson() {
        return getGson().toJson(this);
    }
}
