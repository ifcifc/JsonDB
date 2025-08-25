package com.ifcifc.jsondb;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BinaryDB<T extends BaseModel> extends PersistentStorage<T>{
    //Comprime usando GZip
    private boolean useGZ;

    public BinaryDB(String filepath, Class<T> clazz) {
        this(filepath, clazz, false);
    }

    public BinaryDB(String filepath, Class<T> clazz, boolean useGZ) {
        super(filepath, clazz);

        this.useGZ = useGZ;
        //Cargo la db si existe
        this.secureLock.secureRun(this::integrityCheck);
    }


    @Override
    public void save() {
        this.secureLock.secureRun(()-> {
            try {
                ObjectOutputStream out;
                if(this.useGZ){
                    GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(this.file_db_lock));

                    out = new ObjectOutputStream(gzos);
                }else{
                    out = new ObjectOutputStream(new FileOutputStream(this.file_db_lock));
                }

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
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        this.secureLock.secureRun(()-> {
            try {
                ObjectInputStream in;
                if(this.useGZ){
                    GZIPInputStream gzos = new GZIPInputStream(new FileInputStream(to_load));
                    in = new ObjectInputStream(gzos);
                }else{
                    in = new ObjectInputStream(new FileInputStream(to_load));
                }

                Object[] array = (Object[])in.readObject();
                for(Object obj : array){

                    T _obj = (T)obj;
                    this.db.put(_obj.getId(), _obj);
                }
                in.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
}
