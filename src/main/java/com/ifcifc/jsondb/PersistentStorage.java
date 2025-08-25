package com.ifcifc.jsondb;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PersistentStorage<T extends BaseModel> extends BaseDB<T>  {
    //Archivos de la db
    protected final File file_db; // archivo principal
    protected final File file_db_lock; // lock, donde almacena los cambios antes de cambiar al principal
    protected final File file_db_backup; // backup del principal antes de aplicar los cambios

    protected final Path path_db;
    protected final Path path_db_lock;
    protected final Path path_db_backup;

    public PersistentStorage(String filepath, Class<T> clazz){
        super(clazz);
        this.file_db = new File(filepath);
        this.file_db_lock = new File(filepath+".lock");
        this.file_db_backup = new File(filepath+".bak");

        this.path_db = this.file_db.toPath();
        this.path_db_backup = this.file_db_backup.toPath();
        this.path_db_lock = this.file_db_lock.toPath();
    }

    //Recarga la db
    public void reload(){
        this.secureLock.secureRun(this::integrityCheck);
    }

    //Guarda la db
    public abstract void save();

    //Carga un archivo a la db
    protected abstract void load(File to_load);

    //Carga la db, si encuentra un error carga el backup
    protected void integrityCheck(){
        //Si hay un archivo lock
        if(this.file_db_lock.exists()){
            try {
                System.out.println("Archivo lock detectado: " + this.file_db_lock.getName());
                //lo intento cargar
                this.load(this.file_db_lock);
                //si cargo elimino el backup, ago un backup del actual archivo y remplazo
                //el archivo por el lock
                Files.deleteIfExists(this.path_db_backup);
                if(this.file_db.exists()){
                    Files.move(this.path_db, this.path_db_backup);
                }
                Files.move(this.path_db_lock, this.path_db);
                System.out.println("Archivo lock restaurado");
                return;
            }catch(Exception e){
                System.out.println("El archivo lock no puso ser restaurado: " + e.getMessage());
            }
        }
        try {
            //Si no hay un lock o no pudo se cargado
            //Si existe el archivo
            if (this.file_db.exists()) {
                try {
                    //lo intento cargar
                    System.out.println("Archivo detectado: " + this.file_db.getName());
                    this.load(this.file_db);
                    System.out.println("Archivo cargado");
                    return;
                } catch (Exception e) {
                    System.out.println("El archivo no puso ser cargado: " + e.getMessage());
                }
            }

            //si hay un backup
            if (this.file_db_backup.exists()) {
                try {
                    System.out.println("Archivo backup detectado: " + this.file_db_backup.getName());
                    //intento cargar el backup
                    this.load(this.file_db_backup);
                    //Si cargo el backup correctamente elimino el archivo defectuoso si existe
                    Files.deleteIfExists(this.path_db);
                    //Y lo reemplazo por el backup
                    Files.move(this.path_db_backup, this.path_db);
                    System.out.println("Archivo restaurado");
                } catch (Exception e2) {
                    System.out.println("El archivo no puso ser restaurado: " + e2.getMessage());
                }
            }
        }finally {
            try{
                //Elimino el lock si existe
                Files.deleteIfExists(this.path_db_lock);
            }catch (Exception e){
                System.out.println("El archivo lock no pudo ser eliminado: " + e.getMessage());
            }
        }
    }
}
