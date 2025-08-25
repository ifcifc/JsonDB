package com.ifcifc.test;

import com.ifcifc.jsondb.JsonDB;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            JsonDB<TestModel> db = new JsonDB<TestModel>("test", TestModel.class);

            //Creacion
            TestModel test = db.create(new TestModel("T1"));
            db.save();

            //Edicion
            TestModel test2 = db.create(new TestModel("TX"));
            test2.setName("T2");
            db.save();

            //Clonacion
            TestModel test3 = (TestModel) test2.clone();
            test3.setName("T3");
            db.save();

            //Clonacion && actualizacion
            TestModel test4 = db.create(new TestModel("TX"));
            test4 = (TestModel) test4.clone();
            test4.setName("T4");
            db.update(test4);
            db.save();

            //Eliminacion
            TestModel test5 = db.create(new TestModel("T5"));
            db.delete(test5);
            db.save();

            //Get
            System.out.println("Get: " + db.get(test.getId()));

            //All
            db.all().forEach(System.out::println);

            //Prueba de sincronia
            ArrayList<Thread> t_list = new ArrayList<>();
            for(int i=0;i<50;i++){
                final int t_id = i;
                t_list.add(new Thread(() -> {
                    System.out.println("hilo inicio " + t_id);
                    TestModel tm = db.create(new TestModel("T"+t_id));
                    db.save();
                    System.out.println("hilo fin " + t_id);
                }));
            }

            t_list.forEach(Thread::start);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}