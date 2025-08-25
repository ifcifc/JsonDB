package com.ifcifc.jsondb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class JsonDB<T extends BaseModel> {
    //Archivos de la db
    private final File file_db; // archivo principal
    private final File file_db_lock; // lock, donde almacena los cambios antes de cambiar al principal
    private final File file_db_backup; // backup del principal antes de aplicar los cambios

    private final Path path_db;
    private final Path path_db_lock;
    private final Path path_db_backup;


    private Gson gson;

    //La clase del modelo que se esta trabajando
    private Class<T> clazz;

    //Donde se almacenan los registros
    private HashMap<UUID, T> db;

    //El tipo de dato que se carga desde el json
    private Type type;

    //Lock para evitar condiciones de carrera
    private  SecureLock secureLock;

    public JsonDB(String filepath, Class<T> clazz){
        this.file_db = new File(filepath);
        this.file_db_lock = new File(filepath+".lock");
        this.file_db_backup = new File(filepath+".bak");

        this.path_db = this.file_db.toPath();
        this.path_db_backup = this.file_db_backup.toPath();
        this.path_db_lock = this.file_db_lock.toPath();

        this.clazz = clazz;
        this.secureLock = new SecureLock();
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

    //Carga la db, si encuentra un error carga el backup
    public void integrityCheck(){
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

    //Carga un archivo a la db
    private void load(File to_load){
        this.secureLock.secureRun(()->{
            try (Reader reader = new FileReader(to_load)) {
                this.db = gson.fromJson(reader, type);
            } catch (IOException e) {
                System.out.println("Hubo un error al cargar la db: " + to_load.getName());
            }
        });
    }

    public void reload(){
        //Recargo la db
        this.secureLock.secureRun(this::integrityCheck);
    }

    //Guarda la db
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

    public T get(UUID id){
        return  this.secureLock.secureCall(()->db.getOrDefault(id, null));
    }

    public T update(T entity){
        return this.secureLock.secureCall(()->{
            if(!this.db.containsKey(entity.getId())){
                throw new RuntimeException("Esta entidad no existe en la db");
            }

            this.db.replace(entity.getId(), entity);
            return entity;
        });
    }

    //Obtiene una copia de las entidades
    public Stream<T> all() {
        return this.secureLock.secureCall(()-> this.db.values().stream());
    }

    public T create(T entity) throws RuntimeException{
        return this.secureLock.secureCall(()->{
            try {
                //Genero una nueva instancia del modelo
                T instance = this.clazz.getDeclaredConstructor().newInstance();

                //Busca un id que no este en uso
                UUID id;
                do{
                    id = UUID.randomUUID();
                }while (this.db.containsKey(id));

                //Le paso el id a la entidad nueva
                Field idField = BaseModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(instance, id);

                //Copio los valores a la nueva entidad
                for (Field field : clazz.getDeclaredFields()) {
                    if ("id".equals(field.getName())) continue;
                    field.setAccessible(true);
                    field.set(instance, field.get(entity));
                }

                //Retorno la nueva entidad
                this.db.put(instance.getId(), instance);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean delete(T entity){
        return this.secureLock.secureCall(()-> {
            if (!this.db.containsKey(entity.getId())) return false;
            this.db.remove(entity.getId());
            return true;
        });
    }
}
