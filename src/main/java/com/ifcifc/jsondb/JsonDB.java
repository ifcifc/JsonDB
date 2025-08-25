package com.ifcifc.jsondb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

public class JsonDB<T extends BaseModel> extends PersistentStorage<T> {
    private Gson gson;

    //El tipo de dato que se carga desde el json
    private Type type;

    public JsonDB(String filepath, Class<T> clazz){
        super(filepath, clazz);
        this.type = TypeToken.getParameterized(HashMap.class, UUID.class, clazz).getType();

        //Preparo gson
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();

        //Cargo la db si existe
        this.secureLock.secureRun(this::integrityCheck);
        if(this.db==null){
            this.db = new HashMap<>();
        }
    }

    //Carga un archivo a la db
    @Override
    protected void load(File to_load){
        this.secureLock.secureRun(()->{
            try (Reader reader = new FileReader(to_load)) {
                this.db = gson.fromJson(reader, type);
            } catch (IOException e) {
                System.out.println("Hubo un error al cargar la db: " + to_load.getName());
            }
        });
    }

    //Guarda la db
    @Override
    public void save(){
        this.secureLock.secureRun(()-> {
            try (Writer writer = new FileWriter(this.file_db_lock)) {
                gson.toJson(this.db, writer);
                // Ago el backup antes de intercambiar el lock al principal
                Files.deleteIfExists(this.path_db_backup);
                if(this.file_db.exists()){
                    Files.move(this.path_db, this.path_db_backup);
                }
                Files.move(this.path_db_lock, this.path_db);
            } catch (IOException e) {
                System.out.println("Hubo un error al guardar la db: " + this.file_db.getName());
                e.printStackTrace();
            }
        });
    }


}
