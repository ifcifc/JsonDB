package com.ifcifc.test;
import com.ifcifc.jsondb.BaseModel;

public class TestModel extends BaseModel {
    private String name;

    public TestModel(){}

    public TestModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
