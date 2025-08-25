package com.ifcifc.jsondb;

import java.io.*;
import java.nio.file.Files;

public class BinaryDB<T extends BaseModel> extends PersistentStorage<T>{

    public BinaryDB(String filepath, Class<T> clazz) {
        super(filepath, clazz);

        //Cargo la db si existe
        this.secureLock.secureRun(this::integrityCheck);
    }


    @Override
    public void save() {
        this.secureLock.secureRun(()-> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(this.file_db_lock));
                out.writeObject(this.db.values().toArray());
                out.close();

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

    @Override
    protected void load(File to_load) {
        this.secureLock.secureRun(()-> {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(to_load));
                Object[] array = (Object[])in.readObject();
                for(Object obj : array){
                    T _obj = (T)obj;
                    this.db.put(_obj.getId(), _obj);
                }
                in.close();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
